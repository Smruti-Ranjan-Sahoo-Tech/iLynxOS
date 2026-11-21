#!/usr/bin/env python3
import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageOps

FONT_BOLD = "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"
FONT_REGULAR = "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"
FONT_MONO = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"

def get_font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except Exception:
        return ImageFont.load_default()

def create_gradient(width, height, start_color, end_color):
    base = Image.new("RGBA", (width, height), start_color)
    top = Image.new("RGBA", (width, height), end_color)
    mask = Image.new("L", (width, height))
    mask_data = []
    for y in range(height):
        ratio = y / float(height)
        val = int(255 * ratio)
        mask_data.extend([val] * width)
    mask.putdata(mask_data)
    base.paste(top, (0, 0), mask)
    return base

def add_radial_glow(image, cx, cy, radius, color_rgb, max_alpha=120):
    glow = Image.new("RGBA", image.size, (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    
    # Draw concentric circles with decreasing alpha
    steps = 40
    for i in range(steps, 0, -1):
        r = int(radius * (i / float(steps)))
        alpha = int(max_alpha * (1.0 - (i / float(steps)) ** 1.5))
        c = (color_rgb[0], color_rgb[1], color_rgb[2], alpha)
        glow_draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=c)
    
    glow = glow.filter(ImageFilter.GaussianBlur(radius // 5))
    image.alpha_composite(glow)

def round_corners_with_border(image, radius, border_color=(255, 255, 255, 40), border_width=2):
    mask = Image.new("L", image.size, 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle([0, 0, image.size[0] - 1, image.size[1] - 1], radius=radius, fill=255)
    
    rounded = Image.new("RGBA", image.size, (0, 0, 0, 0))
    rounded.paste(image, (0, 0), mask=mask)
    
    if border_width > 0:
        draw = ImageDraw.Draw(rounded)
        draw.rounded_rectangle([0, 0, image.size[0] - 1, image.size[1] - 1], radius=radius, outline=border_color, width=border_width)
        
    return rounded

def add_drop_shadow(image, offset=(10, 15), blur=25, shadow_color=(0, 0, 0, 160)):
    shadow_size = (image.size[0] + blur * 4, image.size[1] + blur * 4)
    shadow = Image.new("RGBA", shadow_size, (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow)
    
    x0 = blur * 2 + offset[0]
    y0 = blur * 2 + offset[1]
    x1 = x0 + image.size[0]
    y1 = y0 + image.size[1]
    
    shadow_draw.rounded_rectangle([x0, y0, x1, y1], radius=18, fill=shadow_color)
    shadow = shadow.filter(ImageFilter.GaussianBlur(blur))
    
    result = Image.new("RGBA", shadow_size, (0, 0, 0, 0))
    result.paste(shadow, (0, 0))
    result.paste(image, (blur * 2, blur * 2), image)
    return result, (blur * 2, blur * 2)

def create_phone_mockup(screen_img, target_height):
    aspect = screen_img.size[0] / screen_img.size[1]
    sw = int(target_height * aspect)
    sh = target_height
    resized_screen = screen_img.resize((sw, sh), Image.Resampling.LANCZOS)
    
    bw = 14  # bezel width
    bh_top = 22
    bh_bottom = 22
    pw = sw + bw * 2
    ph = sh + bh_top + bh_bottom
    
    phone = Image.new("RGBA", (pw, ph), (0, 0, 0, 0))
    pdraw = ImageDraw.Draw(phone)
    
    # Outer frame
    pdraw.rounded_rectangle([0, 0, pw - 1, ph - 1], radius=32, fill=(28, 32, 42, 255), outline=(70, 80, 105, 255), width=3)
    # Inner rim
    pdraw.rounded_rectangle([2, 2, pw - 3, ph - 3], radius=30, outline=(15, 18, 26, 255), width=2)
    
    # Screen rounded
    screen_rounded = round_corners_with_border(resized_screen, radius=18, border_color=(0, 0, 0, 255), border_width=1)
    phone.paste(screen_rounded, (bw, bh_top), screen_rounded)
    
    # Camera punch hole
    cx = pw // 2
    cy = bh_top + 10
    pdraw.ellipse([cx - 5, cy - 5, cx + 5, cy + 5], fill=(8, 10, 15, 255), outline=(40, 45, 60, 255), width=1)
    
    return phone

def create_desktop_mockup(desktop_img, target_width):
    aspect = desktop_img.size[1] / desktop_img.size[0]
    dw = target_width
    dh = int(target_width * aspect)
    resized_dt = desktop_img.resize((dw, dh), Image.Resampling.LANCZOS)
    
    bar_height = 28
    mw = dw
    mh = dh + bar_height
    
    monitor = Image.new("RGBA", (mw, mh), (0, 0, 0, 0))
    mdraw = ImageDraw.Draw(monitor)
    
    # Window Chrome / Header
    mdraw.rounded_rectangle([0, 0, mw - 1, mh - 1], radius=16, fill=(22, 26, 36, 255), outline=(65, 75, 95, 255), width=2)
    
    # Window controls (red, yellow, green dots)
    dot_y = bar_height // 2
    mdraw.ellipse([16, dot_y - 5, 26, dot_y + 5], fill=(239, 68, 68, 255))
    mdraw.ellipse([34, dot_y - 5, 44, dot_y + 5], fill=(234, 179, 8, 255))
    mdraw.ellipse([52, dot_y - 5, 62, dot_y + 5], fill=(34, 197, 94, 255))
    
    # Title
    tfont = get_font(FONT_BOLD, 12)
    mdraw.text((mw // 2 - 80, dot_y - 7), "Ubuntu 24.04 LTS (Noble) • LXQt Desktop", font=tfont, fill=(160, 175, 200, 255))
    
    # Paste desktop image
    dt_rounded = round_corners_with_border(resized_dt, radius=12, border_color=(40, 48, 65, 255), border_width=1)
    monitor.paste(dt_rounded, (0, bar_height), dt_rounded)
    
    return monitor

# =========================================================================
# 1. INSTAGRAM PROMOTION POST (1080 x 1350 - Vertical 4:5 Optimal Format)
# =========================================================================
def generate_instagram_post():
    W, H = 1080, 1350
    canvas = create_gradient(W, H, (11, 15, 25, 255), (20, 26, 42, 255))
    
    # Atmospheric glows
    add_radial_glow(canvas, W - 150, 180, 420, (255, 107, 0), max_alpha=90)
    add_radial_glow(canvas, 180, 850, 450, (30, 90, 200), max_alpha=70)
    add_radial_glow(canvas, W // 2, H - 120, 350, (255, 107, 0), max_alpha=50)
    
    draw = ImageDraw.Draw(canvas)
    
    # Top Pill Badge
    pill_text = "🚀  100% ROOTLESS  •  STANDALONE LINUX PC"
    pill_font = get_font(FONT_BOLD, 19)
    pill_w = 480
    pill_h = 38
    px0 = (W - pill_w) // 2
    py0 = 50
    draw.rounded_rectangle([px0, py0, px0 + pill_w, py0 + pill_h], radius=19, fill=(255, 107, 0, 30), outline=(255, 107, 0, 220), width=2)
    draw.text((px0 + 26, py0 + 8), pill_text, font=pill_font, fill=(255, 180, 110, 255))
    
    # Headline
    h1_font = get_font(FONT_BOLD, 54)
    h2_font = get_font(FONT_BOLD, 54)
    sub_font = get_font(FONT_REGULAR, 23)
    
    draw.text((W // 2 - 280, 110), "Turn Your Phone", font=h1_font, fill=(255, 255, 255, 255))
    draw.text((W // 2 - 315, 175), "Into a Full Linux PC", font=h2_font, fill=(255, 107, 0, 255))
    
    sub_text = "Ubuntu 24.04 LTS Noble  •  LXQt Modern Desktop  •  In-Process X11"
    draw.text((W // 2 - 355, 245), sub_text, font=sub_font, fill=(160, 175, 200, 255))
    
    # Load and compose screenshots
    desktop_raw = Image.open("images/desktop-image/image1.png").convert("RGBA")
    phone_raw = Image.open("images/application-ui-image/image2.png").convert("RGBA")
    
    desktop_mock = create_desktop_mockup(desktop_raw, target_width=860)
    phone_mock = create_phone_mockup(phone_raw, target_height=560)
    
    dt_shadow, dt_offset = add_drop_shadow(desktop_mock, offset=(0, 20), blur=30, shadow_color=(0, 0, 0, 190))
    ph_shadow, ph_offset = add_drop_shadow(phone_mock, offset=(10, 22), blur=28, shadow_color=(0, 0, 0, 220))
    
    # Paste Desktop (background of visual area)
    dt_x = (W - dt_shadow.size[0]) // 2 + 50
    dt_y = 280
    canvas.alpha_composite(dt_shadow, (dt_x, dt_y))
    
    # Paste Phone (foreground left)
    ph_x = 45
    ph_y = 430
    canvas.alpha_composite(ph_shadow, (ph_x, ph_y))
    
    # 4 Feature Highlight Cards (2x2 Grid)
    card_y = 960
    card_w = 475
    card_h = 100
    card_gap_x = 30
    card_gap_y = 18
    
    features = [
        ("⚡", "Zero External Apps", "No Termux or VNC viewer needed. Single APK."),
        ("🖥️", "In-Process X11 Server", "libXlorie accelerated 60-120Hz display engine."),
        ("📡", "Dual Wireless Desktop", "Cast via TigerVNC (5901) & HTML5 Web (14500)."),
        ("🛍️", "94,000+ Linux Packages", "Full apt repositories, VS Code, Python, Node, Git.")
    ]
    
    cf_title = get_font(FONT_BOLD, 20)
    cf_desc = get_font(FONT_REGULAR, 15)
    
    for i, (icon, title, desc) in enumerate(features):
        col = i % 2
        row = i // 2
        cx0 = 50 + col * (card_w + card_gap_x)
        cy0 = card_y + row * (card_h + card_gap_y)
        
        # Card Background
        draw.rounded_rectangle([cx0, cy0, cx0 + card_w, cy0 + card_h], radius=16, fill=(22, 28, 44, 230), outline=(55, 68, 98, 200), width=1)
        # Left Accent pill
        draw.rounded_rectangle([cx0, cy0, cx0 + 5, cy0 + card_h], radius=3, fill=(255, 107, 0, 255))
        
        # Text
        draw.text((cx0 + 20, cy0 + 16), f"{icon}  {title}", font=cf_title, fill=(255, 255, 255, 255))
        draw.text((cx0 + 24, cy0 + 52), desc, font=cf_desc, fill=(160, 175, 200, 255))
    
    # Bottom Call-to-Action Bar
    bar_y0 = 1215
    bar_h = 85
    draw.rounded_rectangle([50, bar_y0, W - 50, bar_y0 + bar_h], radius=22, fill=(18, 22, 34, 255), outline=(70, 85, 120, 220), width=2)
    
    cta_bold = get_font(FONT_BOLD, 22)
    cta_sub = get_font(FONT_MONO, 17)
    
    draw.text((80, bar_y0 + 18), "⭐ Free & Open Source (GPLv3)", font=cta_bold, fill=(255, 255, 255, 255))
    draw.text((80, bar_y0 + 48), "github.com/Smruti-Ranjan-Sahoo-Tech/iLynxOS", font=cta_sub, fill=(255, 130, 40, 255))
    
    # Download Pill Button on right
    btn_w = 230
    btn_h = 52
    bx0 = W - 50 - btn_w - 20
    by0 = bar_y0 + (bar_h - btn_h) // 2
    draw.rounded_rectangle([bx0, by0, bx0 + btn_w, by0 + btn_h], radius=26, fill=(255, 107, 0, 255))
    btn_font = get_font(FONT_BOLD, 18)
    draw.text((bx0 + 28, by0 + 14), "DOWNLOAD APK", font=btn_font, fill=(255, 255, 255, 255))
    
    out_path = "social/instagram_post.png"
    canvas.save(out_path, "PNG", quality=95)
    print(f"Saved {out_path} ({W}x{H})")
    
    # Also save 1080x1080 square version
    sq_canvas = canvas.crop((0, 0, 1080, 1080))
    sq_canvas.save("social/instagram_square.png", "PNG", quality=95)
    print(f"Saved social/instagram_square.png (1080x1080)")

# =========================================================================
# 2. LINKEDIN PROMOTION BANNER (1200 x 628 - Professional Landscape Format)
# =========================================================================
def generate_linkedin_banner():
    W, H = 1200, 628
    canvas = create_gradient(W, H, (10, 14, 24, 255), (18, 24, 38, 255))
    
    # Accent glows
    add_radial_glow(canvas, 100, 80, 350, (255, 107, 0), max_alpha=70)
    add_radial_glow(canvas, W - 200, 200, 450, (35, 100, 220), max_alpha=85)
    add_radial_glow(canvas, W - 100, H - 80, 300, (255, 107, 0), max_alpha=55)
    
    draw = ImageDraw.Draw(canvas)
    
    # Left Content Column (width ~620px)
    left_x = 60
    
    # Top Tag
    pill_text = "🐧 iLynxOS 1.03  •  NOBLE MODERN EDITION"
    pill_font = get_font(FONT_BOLD, 14)
    draw.rounded_rectangle([left_x, 42, left_x + 350, 74], radius=16, fill=(255, 107, 0, 30), outline=(255, 107, 0, 220), width=1)
    draw.text((left_x + 18, 50), pill_text, font=pill_font, fill=(255, 175, 100, 255))
    
    # Headline
    h1_font = get_font(FONT_BOLD, 36)
    draw.text((left_x, 90), "Turn Any Smartphone", font=h1_font, fill=(255, 255, 255, 255))
    draw.text((left_x, 134), "Into a Full Linux PC.", font=h1_font, fill=(255, 107, 0, 255))
    
    sub_font = get_font(FONT_REGULAR, 17)
    draw.text((left_x, 185), "Standalone, rootless desktop environment powered by Ubuntu 24.04 LTS.", font=sub_font, fill=(160, 175, 200, 255))
    
    # 4 Core Value Bullets
    bullets = [
        "100% Rootless Sandbox — Runs standard Ubuntu ARM64 binaries",
        "In-Process Display Server (libXlorie) — 60/120Hz, no Termux app needed",
        "Dual Wireless Remote — TigerVNC (:1) & HTML5 Web Browser (:14500)",
        "Hardware Mouse & Keyboard — Precise drag-and-drop & trackpad mode"
    ]
    
    b_font = get_font(FONT_REGULAR, 16)
    b_bold = get_font(FONT_BOLD, 16)
    
    for idx, b_text in enumerate(bullets):
        by = 230 + idx * 36
        # Orange checkmark box
        draw.rounded_rectangle([left_x, by + 1, left_x + 20, by + 21], radius=5, fill=(255, 107, 0, 40), outline=(255, 107, 0, 220), width=1)
        draw.text((left_x + 5, by + 1), "✓", font=b_bold, fill=(255, 107, 0, 255))
        draw.text((left_x + 32, by + 1), b_text, font=b_font, fill=(225, 235, 245, 255))
    
    # CTA & GitHub Bar
    bar_y = 410
    draw.rounded_rectangle([left_x, bar_y, left_x + 530, bar_y + 60], radius=16, fill=(20, 26, 40, 240), outline=(60, 75, 105, 220), width=1)
    
    draw.text((left_x + 20, bar_y + 12), "⭐ Free & Open Source (GPLv3)", font=get_font(FONT_BOLD, 16), fill=(255, 255, 255, 255))
    draw.text((left_x + 20, bar_y + 34), "github.com/Smruti-Ranjan-Sahoo-Tech/iLynxOS", font=get_font(FONT_MONO, 12), fill=(255, 130, 40, 255))
    
    btn_w = 150
    btn_h = 42
    bx0 = left_x + 530 - btn_w - 10
    by0 = bar_y + (60 - btn_h) // 2
    draw.rounded_rectangle([bx0, by0, bx0 + btn_w, by0 + btn_h], radius=21, fill=(255, 107, 0, 255))
    draw.text((bx0 + 22, by0 + 12), "GET APK ➔", font=get_font(FONT_BOLD, 14), fill=(255, 255, 255, 255))
    
    # Quote at bottom left
    q_font = get_font(FONT_REGULAR, 12)
    draw.text((left_x, 500), '"A computer shouldn\'t be defined by its shape. It should be defined by what it empowers you to do."', font=q_font, fill=(120, 135, 160, 255))
    draw.text((left_x, 520), '— Built by Smruti Ranjan Sahoo', font=get_font(FONT_BOLD, 12), fill=(255, 107, 0, 220))
    
    # Right Visual Side (Desktop + Phone Mockup)
    desktop_raw = Image.open("images/desktop-image/image1.png").convert("RGBA")
    phone_raw = Image.open("images/application-ui-image/image2.png").convert("RGBA")
    
    desktop_mock = create_desktop_mockup(desktop_raw, target_width=570)
    phone_mock = create_phone_mockup(phone_raw, target_height=420)
    
    dt_shadow, dt_offset = add_drop_shadow(desktop_mock, offset=(5, 15), blur=24, shadow_color=(0, 0, 0, 200))
    ph_shadow, ph_offset = add_drop_shadow(phone_mock, offset=(8, 16), blur=22, shadow_color=(0, 0, 0, 220))
    
    # Desktop position
    dt_x = 615
    dt_y = 60
    canvas.alpha_composite(dt_shadow, (dt_x, dt_y))
    
    # Phone position (overlapping front-left of desktop)
    ph_x = 555
    ph_y = 150
    canvas.alpha_composite(ph_shadow, (ph_x, ph_y))
    
    out_path = "social/linkedin_post.png"
    canvas.save(out_path, "PNG", quality=95)
    print(f"Saved {out_path} ({W}x{H})")

if __name__ == "__main__":
    generate_instagram_post()
    generate_linkedin_banner()

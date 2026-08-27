import os
import math
from PIL import Image, ImageDraw

res_dir = "/mnt/newvolume/liquid-expense-manager/app/src/main/res"

densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

def render_exact_lqd_logo_v7(size, is_round=False):
    scale = 8
    S = size * scale
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 1. Dark Squircle Container
    pad = int(S * 0.038)
    r = S // 2 if is_round else int(S * 0.22)
    bg_box = [pad, pad, S - pad, S - pad]
    
    draw.rounded_rectangle(
        bg_box,
        radius=r,
        fill=(14, 16, 20, 255),
        outline=(30, 34, 44, 255),
        width=max(2, int(S * 0.012))
    )

    # 2. Exact Monogram Geometry
    W = S * 0.092

    # Total monogram size centered in squircle
    mono_h = S * 0.52
    mono_w = S * 0.52
    top = (S - mono_h) / 2
    bottom = top + mono_h
    left = (S - mono_w) / 2
    right = left + mono_w

    # Q circle dimensions (diameter is ~0.70 of height)
    q_diam = mono_h * 0.70
    q_right = right
    q_left = right - q_diam
    q_top = top
    q_bottom = top + q_diam

    cx_q = (q_left + q_right) / 2
    cy_q = (q_top + q_bottom) / 2
    r_outer = q_diam / 2
    r_inner = r_outer - W

    # Step 1: Draw L Vertical Stem
    draw.rectangle([left, top, left + W, bottom], fill=(255, 255, 255, 255))

    # Step 2: Draw Q Outer Circle
    draw.ellipse([q_left, q_top, q_right, q_bottom], fill=(255, 255, 255, 255))

    # Step 3: Draw Q Diagonal Tail (45 degrees angled bar)
    angle = math.radians(45)
    dx = math.cos(angle)
    dy = math.sin(angle)
    px = -dy
    py = dx

    t_start_x = cx_q + (r_inner * 0.75) * dx
    t_start_y = cy_q + (r_inner * 0.75) * dy
    t_end_x = cx_q + (r_outer * 1.55) * dx
    t_end_y = cy_q + (r_outer * 1.55) * dy

    half_w = W / 2
    c1 = (t_start_x + half_w * px, t_start_y + half_w * py)
    c2 = (t_start_x - half_w * px, t_start_y - half_w * py)
    c3 = (t_end_x - half_w * px, t_end_y - half_w * py)
    c4 = (t_end_x + half_w * px, t_end_y + half_w * py)

    draw.polygon([c1, c2, c3, c4], fill=(255, 255, 255, 255))

    # Step 4: Draw L Horizontal Base connecting stem all the way to meet tail's left edge
    tail_left_edge_x = c3[0]
    draw.rectangle([left + W, bottom - W, tail_left_edge_x, bottom], fill=(255, 255, 255, 255))

    # Step 5: Cutout Inner Hole of Q Circle
    draw.ellipse([q_left + W, q_top + W, q_right - W, q_bottom - W], fill=(14, 16, 20, 255))

    return img.resize((size, size), Image.Resampling.LANCZOS)

preview = render_exact_lqd_logo_v7(512, is_round=False)
preview.save(os.path.join(res_dir, "ic_launcher-playstore.png"), "PNG")

for folder, size in densities.items():
    dir_path = os.path.join(res_dir, folder)
    os.makedirs(dir_path, exist_ok=True)
    
    icon_sq = render_exact_lqd_logo_v7(size, is_round=False)
    icon_sq.save(os.path.join(dir_path, "ic_launcher.png"), "PNG")
    
    icon_rd = render_exact_lqd_logo_v7(size, is_round=True)
    icon_rd.save(os.path.join(dir_path, "ic_launcher_round.png"), "PNG")

print("Generated v7 exact LQD logo!")

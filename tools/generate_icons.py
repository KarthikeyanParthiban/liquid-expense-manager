import os
from PIL import Image, ImageDraw, ImageFont

res_dir = "/mnt/newvolume/liquid-expense-manager/app/src/main/res"

densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

def draw_kaching_icon(size, is_round=False):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    pad = int(size * 0.04)
    r = size // 2 if is_round else int(size * 0.22)
    
    # Outer obsidian background
    bg_box = [pad, pad, size - pad, size - pad]
    draw.rounded_rectangle(bg_box, radius=r, fill=(18, 18, 24, 255), outline=(45, 45, 58, 255), width=max(1, size // 48))
    
    # Inner subtle radial/gradient glow
    glow_pad = int(size * 0.15)
    draw.ellipse([glow_pad, glow_pad, size - glow_pad, size - glow_pad], fill=(24, 32, 28, 180))
    
    # Draw geometric "K" / Cash Spark Emblem
    cx, cy = size / 2, size / 2
    w = size * 0.42
    h = size * 0.46
    
    # Left vertical stem
    stem_w = max(2, int(size * 0.08))
    x0 = cx - w * 0.45
    x1 = x0 + stem_w
    y0 = cy - h * 0.45
    y1 = cy + h * 0.45
    draw.rounded_rectangle([x0, y0, x1, y1], radius=stem_w // 2, fill=(255, 255, 255, 255))
    
    # Upper diagonal arm of K
    p_up_start = (x1 - stem_w * 0.2, cy - h * 0.05)
    p_up_end = (cx + w * 0.45, y0)
    draw.line([p_up_start, p_up_end], fill=(16, 185, 129, 255), width=stem_w)
    
    # Lower diagonal arm of K
    p_down_start = (x1 - stem_w * 0.2, cy - h * 0.05)
    p_down_end = (cx + w * 0.45, y1)
    draw.line([p_down_start, p_down_end], fill=(255, 255, 255, 255), width=stem_w)
    
    # Little emerald spark dot at top-right
    spark_r = max(2, int(size * 0.05))
    spark_cx = cx + w * 0.42
    spark_cy = y0 - spark_r * 0.3
    draw.ellipse([spark_cx - spark_r, spark_cy - spark_r, spark_cx + spark_r, spark_cy + spark_r], fill=(16, 185, 129, 255))

    return img

for folder, size in densities.items():
    dir_path = os.path.join(res_dir, folder)
    os.makedirs(dir_path, exist_ok=True)
    
    # Square icon
    icon_sq = draw_kaching_icon(size, is_round=False)
    icon_sq.save(os.path.join(dir_path, "ic_launcher.png"), "PNG")
    
    # Round icon
    icon_rd = draw_kaching_icon(size, is_round=True)
    icon_rd.save(os.path.join(dir_path, "ic_launcher_round.png"), "PNG")

print("Generated all mipmap PNG icons successfully!")

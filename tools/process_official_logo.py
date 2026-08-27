import os
from PIL import Image, ImageDraw

src_path = "/home/karthikeyan/Downloads/ChatGPT Image Aug 27, 2026, 06_19_41 AM.png"
res_dir = "/mnt/newvolume/liquid-expense-manager/app/src/main/res"

densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Open original user image
img = Image.open(src_path).convert("RGBA")
bbox = img.getbbox()
print("Exact Logo Bounding Box:", bbox)

# Crop the exact squircle
squircle = img.crop(bbox)
sq_w, sq_h = squircle.size
dim = max(sq_w, sq_h)

# Place into square canvas with transparent padding
sq_canvas = Image.new("RGBA", (dim, dim), (0, 0, 0, 0))
off_x = (dim - sq_w) // 2
off_y = (dim - sq_h) // 2
sq_canvas.paste(squircle, (off_x, off_y))

# 1. Generate 512x512 Play Store Asset
high_res_sq = sq_canvas.resize((512, 512), Image.Resampling.LANCZOS)
high_res_sq.save(os.path.join(res_dir, "ic_launcher-playstore.png"), "PNG")
print("Saved 512x512 Play Store icon")

# 2. Prepare Round Version
# For round launcher icons, mask inside a circle
circle_mask = Image.new("L", (dim, dim), 0)
draw_mask = ImageDraw.Draw(circle_mask)
pad = int(dim * 0.02)
draw_mask.ellipse([pad, pad, dim - pad, dim - pad], fill=255)

round_canvas = Image.new("RGBA", (dim, dim), (0, 0, 0, 0))
round_canvas.paste(sq_canvas, (0, 0), mask=circle_mask)

# 3. Generate all Mipmap Densities
for folder, size in densities.items():
    dir_path = os.path.join(res_dir, folder)
    os.makedirs(dir_path, exist_ok=True)
    
    # Square icon
    out_sq = sq_canvas.resize((size, size), Image.Resampling.LANCZOS)
    out_sq.save(os.path.join(dir_path, "ic_launcher.png"), "PNG")
    
    # Round icon
    out_rd = round_canvas.resize((size, size), Image.Resampling.LANCZOS)
    out_rd.save(os.path.join(dir_path, "ic_launcher_round.png"), "PNG")

# 4. Generate Adaptive Icon Foreground & Background
# Foreground is the centered white monogram inside 108dp canvas (inner 66dp is safe zone)
# Extract only white/bright pixels (the LQ monogram) on transparent background
mono_img = Image.new("RGBA", (dim, dim), (0, 0, 0, 0))
sq_pixels = sq_canvas.load()
mono_pixels = mono_img.load()

for y in range(dim):
    for x in range(dim):
        r, g, b, a = sq_pixels[x, y]
        if a > 0 and (r > 150 or g > 150 or b > 150):
            # Pure crisp white monogram
            mono_pixels[x, y] = (255, 255, 255, a)

# Adaptive foreground: 432x432 for xxxhdpi (108dp * 4) with 60% scale centered
adaptive_fg_size = 432
adaptive_fg = Image.new("RGBA", (adaptive_fg_size, adaptive_fg_size), (0, 0, 0, 0))
mono_scaled_size = int(adaptive_fg_size * 0.58)
mono_resized = mono_img.resize((mono_scaled_size, mono_scaled_size), Image.Resampling.LANCZOS)
fg_offset = (adaptive_fg_size - mono_scaled_size) // 2
adaptive_fg.paste(mono_resized, (fg_offset, fg_offset))

# Save adaptive icon foreground PNG in drawable
adaptive_fg.save(os.path.join(res_dir, "drawable", "ic_launcher_foreground_bitmap.png"), "PNG")

print("Successfully generated all official icons from user's file!")

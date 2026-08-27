from PIL import Image

src_path = "/home/karthikeyan/Downloads/ChatGPT Image Aug 27, 2026, 06_19_41 AM.png"
img = Image.open(src_path).convert("RGB")
w, h = img.size

# Let's check where the white border is and where the black squircle is:
# We know the black squircle is dark (e.g. < 40), white background is > 200, and white letters inside are > 200.
# Find the boundary of the black squircle:
# In the center (w//2, h//2), it's black (11, 10, 13).
# Let's trace from center in 4 directions to find where black turns white at the outer border:

cx, cy = w // 2, h // 2
print(f"Center ({cx}, {cy}): {img.getpixel((cx, cy))}")

# Trace up
top_y = 0
for y in range(cy, 0, -1):
    r, g, b = img.getpixel((cx, y))
    # if it's white letter inside, continue, but if we go past letter to black and then to white outside:
    # Let's check
    pass

# Let's sample along horizontal line y = cy
for x in range(0, w, 50):
    print(f"x={x}, y={cy}: {img.getpixel((x, cy))}")

# Let's sample along horizontal line y = 100
for x in range(0, w, 50):
    print(f"x={x}, y=100: {img.getpixel((x, 100))}")

# Check corner (10, 10)
print(f"(10, 10): {img.getpixel((10, 10))}")
print(f"(100, 100): {img.getpixel((100, 100))}")
print(f"(w//2, 10): {img.getpixel((w//2, 10))}")

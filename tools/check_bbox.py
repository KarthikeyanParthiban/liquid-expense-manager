from PIL import Image

src_path = "/home/karthikeyan/Downloads/ChatGPT Image Aug 27, 2026, 06_19_41 AM.png"
img = Image.open(src_path)
print("Image format:", img.format, "mode:", img.mode, "size:", img.size)

# If it has an alpha channel, getbbox() directly gets the bounding box:
bbox = img.getbbox()
print("getbbox():", bbox)

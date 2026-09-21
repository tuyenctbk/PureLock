import os

res_dir = "/Users/user/antigravity/PureLock/app/src/main/res"

# 1. Purge legacy raster images from mipmap folders
folders = [
    "mipmap-mdpi",
    "mipmap-hdpi",
    "mipmap-xhdpi",
    "mipmap-xxhdpi",
    "mipmap-xxxhdpi",
]

for folder in folders:
    folder_path = os.path.join(res_dir, folder)
    if os.path.exists(folder_path):
        for file_name in os.listdir(folder_path):
            if file_name.endswith(".webp") or file_name.endswith(".png"):
                file_path = os.path.join(folder_path, file_name)
                os.remove(file_path)
                print(f"Purged raster mipmap asset: {file_path}")

# 2. Purge PNG images from drawable folder
drawable_dir = os.path.join(res_dir, "drawable")
if os.path.exists(drawable_dir):
    for file_name in os.listdir(drawable_dir):
        if file_name.endswith(".png"):
            file_path = os.path.join(drawable_dir, file_name)
            os.remove(file_path)
            print(f"Purged PNG drawable asset: {file_path}")

print("All PNG and raster assets removed! App strictly uses 100% pure XML Vector Drawables.")

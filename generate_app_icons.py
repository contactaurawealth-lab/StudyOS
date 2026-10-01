import os
import math
from PIL import Image, ImageDraw, ImageFilter

def generate_all_icons():
    # 1. Load the modern logo & transparent overlay
    modern_img = Image.open("/root/StudyOS/study_logo_modern.png").convert("RGBA")
    trans_img = Image.open("/root/StudyOS/study_logo_transparent.png").convert("RGBA")
    
    # --- Android Adaptive Icon Foreground ---
    # Canvas size: 432x432 px
    # Safe zone: center (216, 216), radius 144 px
    # To guarantee NO clipping on circular masks, all opaque pixels must have hypot(x-216, y-216) <= 136
    fg_canvas = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
    
    bbox = trans_img.getbbox()
    cropped_artwork = trans_img.crop(bbox)
    art_w, art_h = cropped_artwork.size
    
    # Scale so that maximum corner distance from center <= 136
    # Current max distance at target_max_dim=240 was 149.81
    # Scale factor = 136 / 149.81 * (240 / max_dim)
    target_max_dim = int(240 * (134.0 / 149.81)) # ~214 px
    scale = target_max_dim / max(art_w, art_h)
    new_w = int(art_w * scale)
    new_h = int(art_h * scale)
    scaled_artwork = cropped_artwork.resize((new_w, new_h), Image.Resampling.LANCZOS)
    
    paste_x = (432 - new_w) // 2
    paste_y = (432 - new_h) // 2
    fg_canvas.paste(scaled_artwork, (paste_x, paste_y), scaled_artwork)
    
    fg_path = "/root/StudyOS/app/src/main/res/drawable/ic_launcher_foreground.png"
    fg_canvas.save(fg_path, "PNG")
    print(f"Saved {fg_path} (432x432, safe scale: {target_max_dim}px)")

    # --- Android Mipmaps (Square & Round) ---
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }

    for folder, size in densities.items():
        dir_path = os.path.join("/root/StudyOS/app/src/main/res", folder)
        os.makedirs(dir_path, exist_ok=True)
        
        # 1. Standard Squircle / Rounded Square Icon
        sq_img = modern_img.resize((size, size), Image.Resampling.LANCZOS)
        sq_path = os.path.join(dir_path, "ic_launcher.png")
        sq_img.save(sq_path, "PNG")
        
        # 2. Round Icon (Circle Masked)
        circle_mask = Image.new("L", (size, size), 0)
        c_draw = ImageDraw.Draw(circle_mask)
        c_draw.ellipse([0, 0, size, size], fill=255)
        
        round_canvas = Image.new("RGBA", (size, size), (11, 15, 25, 255))
        # Place scaled artwork inside circle (leaving 18% margin)
        inner_max = int(size * 0.65)
        scale_inner = inner_max / max(art_w, art_h)
        inner_w = int(art_w * scale_inner)
        inner_h = int(art_h * scale_inner)
        inner_art = cropped_artwork.resize((inner_w, inner_h), Image.Resampling.LANCZOS)
        ix = (size - inner_w) // 2
        iy = (size - inner_h) // 2
        round_canvas.paste(inner_art, (ix, iy), inner_art)
        
        round_final = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        round_final.paste(round_canvas, (0, 0), mask=circle_mask)
        
        round_path = os.path.join(dir_path, "ic_launcher_round.png")
        round_final.save(round_path, "PNG")

    # --- Web Assets ---
    web_logo = modern_img.resize((512, 512), Image.Resampling.LANCZOS)
    web_logo.save("/root/StudyOS/web/studyos_logo.png", "PNG")
    
    web_fav = modern_img.resize((64, 64), Image.Resampling.LANCZOS)
    web_fav.save("/root/StudyOS/web/favicon.png", "PNG")

    # --- Docs Assets ---
    docs_logo = modern_img.resize((512, 512), Image.Resampling.LANCZOS)
    docs_logo.save("/root/StudyOS/docs/logo.png", "PNG")
    docs_logo.save("/root/StudyOS/docs/assets/logo.png", "PNG")

    docs_fav = modern_img.resize((64, 64), Image.Resampling.LANCZOS)
    docs_fav.save("/root/StudyOS/docs/favicon.png", "PNG")

    docs_touch = modern_img.resize((180, 180), Image.Resampling.LANCZOS)
    docs_touch.save("/root/StudyOS/docs/apple-touch-icon.png", "PNG")
    print("All Android, Web, and Docs icon assets generated successfully!")

if __name__ == "__main__":
    generate_all_icons()

import os
import math
from PIL import Image, ImageDraw, ImageFont

# Brand Colors
COLOR_DARK = "#121212"
COLOR_LIGHT = "#FFFFFF"
COLOR_ACCENT = "#00E5FF" # Electric cyan

def create_canvas(size=2048, bg_color=(0, 0, 0, 0)):
    return Image.new("RGBA", (size, size), bg_color)

def draw_geometric_95(draw, offset_x, offset_y, scale, color_9, color_5, accent_color):
    """
    Draws a highly custom, geometric 95 using primitives.
    """
    t = int(140 * scale) # thickness
    
    # --- Draw '9' ---
    x9 = offset_x
    y9 = offset_y
    w9 = int(500 * scale)
    h9 = int(800 * scale)
    
    # 9's top loop (circle)
    draw.ellipse([x9, y9, x9+w9, y9+w9], outline=color_9, width=t)
    # 9's stem (right side vertical line)
    draw.rectangle([x9+w9-t/2, y9+w9/2, x9+w9+t/2, y9+h9], fill=color_9)
    # 9's bottom curve (optional, keeping it minimal and straight-edged at bottom is modern)
    
    # --- Draw '5' ---
    gap = int(120 * scale)
    x5 = x9 + w9 + gap
    y5 = offset_y
    w5 = int(480 * scale)
    
    # 5's top bar
    draw.rectangle([x5, y5, x5+w5, y5+t], fill=color_5)
    # 5's left stem
    stem_h = int(350 * scale)
    draw.rectangle([x5, y5, x5+t, y5+stem_h], fill=color_5)
    
    # 5's bottom loop
    loop_y = y5 + stem_h - t/2
    loop_h = int(450 * scale)
    # Draw arc for the 5's belly
    # Instead of full arc, we draw the arc and then slice it with an arrow
    bbox_5 = [x5 - w5*0.1, loop_y, x5 + w5, loop_y + loop_h]
    draw.arc(bbox_5, start=-90, end=45, fill=color_5, width=t)
    
    # The Arrow embedded in the 5 (pointing Up-Right)
    # Calculate position of 45 degrees on the arc
    cx = (bbox_5[0] + bbox_5[2]) / 2
    cy = (bbox_5[1] + bbox_5[3]) / 2
    r_x = (bbox_5[2] - bbox_5[0]) / 2
    r_y = (bbox_5[3] - bbox_5[1]) / 2
    
    angle = math.radians(45)
    end_x = cx + r_x * math.cos(angle)
    end_y = cy + r_y * math.sin(angle)
    
    # Arrow head polygon
    arrow_size = int(180 * scale)
    pt1 = (end_x, end_y - t/2)
    pt2 = (end_x + arrow_size, end_y - arrow_size)
    pt3 = (end_x + arrow_size, end_y + arrow_size*0.2)
    pt4 = (end_x - arrow_size*0.2, end_y - arrow_size)
    
    # Draw the arrow in accent color to pop, or same color for mono
    arrow_color = accent_color if accent_color else color_5
    
    # To make it point purely UP-RIGHT seamlessly:
    aw = int(220 * scale)
    ax = x5 + w5 - aw/2
    ay = loop_y + loop_h/2 + int(80*scale)
    
    # Draw custom arrowhead polygon
    poly = [
        (ax, ay), 
        (ax + aw, ay - aw), 
        (ax + aw, ay),
        (ax + int(aw*0.4), ay + int(aw*0.6))
    ]
    draw.polygon([(ax-aw*0.2, ay+aw*0.2), (ax+aw*1.2, ay-aw*1.2), (ax+aw*1.2, ay+aw), (ax, ay+aw)], fill=arrow_color)


def build_logo_variations():
    os.makedirs("logo/sizes", exist_ok=True)
    
    # Helper to generate and downsample
    def render(bg, color9, color5, accent, filename, is_icon=False, draw_wordmark=False):
        # Render at 4x resolution (4096) for supersampling down to 1024
        sz = 4096
        img = create_canvas(sz, bg)
        draw = ImageDraw.Draw(img)
        
        # Center the mark
        scale = 1.8
        total_width = int((500 + 120 + 480) * scale)
        start_x = (sz - total_width) / 2
        start_y = sz / 2 - int(400 * scale)
        
        draw_geometric_95(draw, start_x, start_y, scale, color9, color5, accent)
        
        if draw_wordmark:
            # We would draw 'OS' here
            try:
                font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", int(350 * scale))
                draw.text((start_x + total_width + int(80*scale), start_y + int(300*scale)), "OS", fill=color5, font=font)
            except:
                pass # fallback if no font
                
        # Resize to target
        out_sz = 1024
        img = img.resize((out_sz, out_sz), Image.Resampling.LANCZOS)
        
        if is_icon:
            # App icon container (rounded square)
            icon_bg = Image.new("RGBA", (out_sz, out_sz), (0,0,0,0))
            draw_bg = ImageDraw.Draw(icon_bg)
            rad = 225
            draw_bg.rounded_rectangle([0,0,out_sz,out_sz], radius=rad, fill=bg)
            
            # Paste scaled down logo inside
            inner_size = int(out_sz * 0.7)
            img_small = img.resize((inner_size, inner_size), Image.Resampling.LANCZOS)
            offset = (out_sz - inner_size) // 2
            icon_bg.alpha_composite(img_small, (offset, offset))
            icon_bg.save(f"logo/{filename}")
            return icon_bg
        else:
            img.save(f"logo/{filename}")
            return img

    # Generate variants
    # 1. Mark (Transparent)
    mark_img = render((0,0,0,0), COLOR_DARK, COLOR_DARK, COLOR_ACCENT, "95os-mark.png")
    
    # 2. Mark Mono
    render((0,0,0,0), COLOR_DARK, COLOR_DARK, COLOR_DARK, "95os-mark-mono.png")
    
    # 3. Wordmark
    render((0,0,0,0), COLOR_DARK, COLOR_DARK, COLOR_ACCENT, "95os-wordmark.png", draw_wordmark=True)
    
    # 4. Icon Light
    render(COLOR_LIGHT, COLOR_DARK, COLOR_DARK, COLOR_ACCENT, "95os-icon.png", is_icon=True)
    
    # 5. Icon Dark
    render(COLOR_DARK, COLOR_LIGHT, COLOR_LIGHT, COLOR_ACCENT, "95os-icon-dark.png", is_icon=True)
    
    # 6. Preview Light
    render(COLOR_LIGHT, COLOR_DARK, COLOR_DARK, COLOR_ACCENT, "95os-preview-light.png")
    
    # 7. Preview Dark
    render(COLOR_DARK, COLOR_LIGHT, COLOR_LIGHT, COLOR_ACCENT, "95os-preview-dark.png")
    
    # Generate Sizes for Mark
    for size in [1024, 512, 256, 128, 64, 32]:
        resized = mark_img.resize((size, size), Image.Resampling.LANCZOS)
        resized.save(f"logo/sizes/{size}.png")

if __name__ == '__main__':
    build_logo_variations()
    print("Logo generation complete!")

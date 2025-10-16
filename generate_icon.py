#!/usr/bin/env python3
"""Generate Android app icons from scratch"""

from PIL import Image, ImageDraw, ImageFont
import os

def create_gradient_icon():
    """Create a TP icon with blue-green gradient"""
    size = 1024
    img = Image.new('RGB', (size, size))
    draw = ImageDraw.Draw(img)

    # Create gradient from blue to green
    for y in range(size):
        # Interpolate between blue (#4084f0) and green (#40e8a0)
        r = int(64)
        g = int(132 + (232 - 132) * y / size)
        b = int(240 - (240 - 160) * y / size)
        draw.line([(0, y), (size, y)], fill=(r, g, b))

    # Add "TP" text
    try:
        # Try to use a bold font
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 700)
    except:
        # Fallback to default font
        font = ImageFont.load_default()

    text = "TP"
    # Get text bounding box
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]

    # Center text
    x = (size - text_width) // 2 - bbox[0]
    y = (size - text_height) // 2 - bbox[1]

    draw.text((x, y), text, fill='white', font=font)

    return img

def generate_launcher_icons(base_img, res_dir):
    """Generate launcher icons for all densities"""
    densities = {
        'mdpi': 48,
        'hdpi': 72,
        'xhdpi': 96,
        'xxhdpi': 144,
        'xxxhdpi': 192
    }

    for density, size in densities.items():
        mipmap_dir = os.path.join(res_dir, f'mipmap-{density}')
        os.makedirs(mipmap_dir, exist_ok=True)

        # Resize and save
        resized = base_img.resize((size, size), Image.Resampling.LANCZOS)

        # Save both launcher and round icons
        resized.save(os.path.join(mipmap_dir, 'ic_launcher.png'))
        resized.save(os.path.join(mipmap_dir, 'ic_launcher_round.png'))

        print(f"Created icons for {density}: {size}x{size}")

def main():
    # Create base icon
    print("Creating base icon...")
    base_icon = create_gradient_icon()

    # Save high-res version
    base_icon.save('source_icon.png')
    print("Saved source_icon.png")

    # Generate all launcher icons
    res_dir = 'app/src/main/res'
    print(f"\nGenerating launcher icons in {res_dir}...")
    generate_launcher_icons(base_icon, res_dir)

    print("\n✓ All icons generated successfully!")

if __name__ == '__main__':
    main()


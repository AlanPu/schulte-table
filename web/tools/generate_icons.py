#!/usr/bin/env python3
"""生成舒尔特表 PWA 图标（纯 Python，无第三方依赖）。

设计：深色底 + 3×3 彩色圆角方块网格，呼应 App 内的彩色格子。
使用 3 倍超采样 + 盒式降采样抗锯齿。

用法：python3 generate_icons.py
"""

import os
import struct
import zlib

PALETTE = [
    (0xE5, 0x39, 0x35),  # 红
    (0xFB, 0x8C, 0x00),  # 橙
    (0xFD, 0xD8, 0x35),  # 黄
    (0x43, 0xA0, 0x47),  # 绿
    (0x1E, 0x88, 0xE5),  # 蓝
    (0x8E, 0x24, 0xAA),  # 紫
    (0xD8, 0x1B, 0x60),  # 粉
    (0x5E, 0x35, 0xB1),  # 靛
]

BG = (0x16, 0x17, 0x1C)

# 3×3 取色顺序
LAYOUT = [0, 1, 2, 3, 4, 5, 6, 7, 0]

SUPERSAMPLE = 3


def rounded_rect_coverage(px, py, x0, y0, x1, y1, r):
    """判断点是否落在圆角矩形内。"""
    if px < x0 or px > x1 or py < y0 or py > y1:
        return False
    # 四角区域做圆检测
    cx = min(max(px, x0 + r), x1 - r)
    cy = min(max(py, y0 + r), y1 - r)
    dx = px - cx
    dy = py - cy
    return dx * dx + dy * dy <= r * r


def render(size, content_ratio):
    """返回 RGBA 像素字节（越界处透明，用于 maskable 内边距）。"""
    h = size * SUPERSAMPLE
    tiles = 3
    gap = h * 0.045
    side = h * content_ratio
    origin = (h - side) / 2
    tile = (side - gap * (tiles - 1)) / tiles
    radius = tile * 0.24

    canvas = bytearray(h * h * 4)

    # 底色铺满（含圆角外的部分，iOS/浏览器自行裁切）
    for i in range(h * h):
        o = i * 4
        canvas[o] = BG[0]
        canvas[o + 1] = BG[1]
        canvas[o + 2] = BG[2]
        canvas[o + 3] = 255

    for idx in range(tiles * tiles):
        row, col = divmod(idx, tiles)
        color = PALETTE[LAYOUT[idx] % len(PALETTE)]

        x0 = origin + col * (tile + gap)
        y0 = origin + row * (tile + gap)
        x1 = x0 + tile
        y1 = y0 + tile

        px0, px1 = max(0, int(x0)), min(h, int(x1) + 2)
        py0, py1 = max(0, int(y0)), min(h, int(y1) + 2)

        for py in range(py0, py1):
            row_off = py * h * 4
            for px in range(px0, px1):
                if rounded_rect_coverage(px + 0.5, py + 0.5, x0, y0, x1, y1, radius):
                    o = row_off + px * 4
                    canvas[o] = color[0]
                    canvas[o + 1] = color[1]
                    canvas[o + 2] = color[2]
                    canvas[o + 3] = 255

    # 盒式降采样
    out = bytearray(size * size * 4)
    n = SUPERSAMPLE * SUPERSAMPLE
    for y in range(size):
        for x in range(size):
            r = g = b = a = 0
            for dy in range(SUPERSAMPLE):
                base = ((y * SUPERSAMPLE + dy) * h + x * SUPERSAMPLE) * 4
                for dx in range(SUPERSAMPLE):
                    o = base + dx * 4
                    r += canvas[o]
                    g += canvas[o + 1]
                    b += canvas[o + 2]
                    a += canvas[o + 3]
            o = (y * size + x) * 4
            out[o] = r // n
            out[o + 1] = g // n
            out[o + 2] = b // n
            out[o + 3] = a // n
    return bytes(out)


def write_png(path, size, pixels):
    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        return c + struct.pack('>I', zlib.crc32(tag + data) & 0xFFFFFFFF)

    raw = bytearray()
    stride = size * 4
    for y in range(size):
        raw.append(0)  # filter type 0
        raw += pixels[y * stride:(y + 1) * stride]

    ihdr = struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0)
    png = (b'\x89PNG\r\n\x1a\n'
           + chunk(b'IHDR', ihdr)
           + chunk(b'IDAT', zlib.compress(bytes(raw), 9))
           + chunk(b'IEND', b''))

    with open(path, 'wb') as f:
        f.write(png)


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    targets = [
        ('icon-180.png', 180, 0.86),   # iOS 主屏幕
        ('icon-192.png', 192, 0.86),   # Android / PWA
        ('icon-512.png', 512, 0.86),   # 安装图标
        ('icon-512-maskable.png', 512, 0.62),  # maskable 安全区
        ('icon-1024.png', 1024, 0.86),
    ]
    for name, size, ratio in targets:
        path = os.path.join(here, name)
        write_png(path, size, render(size, ratio))
        print(f'✓ {name}  {size}×{size}  {os.path.getsize(path) / 1024:.1f} KB')


if __name__ == '__main__':
    main()

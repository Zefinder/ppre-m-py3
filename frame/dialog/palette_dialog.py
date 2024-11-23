from math import floor
from PyQt6.QtWidgets import QDialog, QLabel, QHBoxLayout
from PyQt6.QtGui import QPixmap, QPainter, QPen, QColor

class PaletteDialog(QDialog):
    def __init__(self, palettes: list[list[int]]) -> None:
        """
        Creates a dialog to show the specified palette. This can be called by 
        opening a palette file or by opening an image using the palette.
        Remember that the color format is XBBBBBGGGGGRRRRR (BGR555).

        Args:
            palettes (list[list[int]]): Palette to show
        """
        super().__init__()
        color_size = 20

        # Set title with number of palettes and bit depth
        if len(palettes) == 1 and len(palettes[0]) == 256:
            self.setWindowTitle("Palette - 1 palette, 256 colors (8-bits depth)")
            colors = self.process_8bits_palette(palettes[0])
        else:
            self.setWindowTitle(f"Palette - {len(palettes):d} palette(s), 16 colors (4-bits depth)")
            colors = self.process_4bit_palettes(palettes)

        # Create frame things
        label = QLabel()
        canvas = QPixmap(16 * color_size, 16 * color_size)
        canvas.fill(QColor(255, 255, 255))
        label.setPixmap(canvas)
        painter = QPainter(canvas)

        x = 0
        y = 0
        for palette in colors:
            for color in palette:
                painter.setBrush(color)
                painter.drawRect(x, y, color_size, color_size)
                x += color_size

            x = 0
            y += color_size

        painter.end()
        label.setPixmap(canvas)

        layout = QHBoxLayout()
        layout.addWidget(label)
        self.setLayout(layout)


    def bgr555_to_rgb(self, bgr: int) -> tuple[int, int, int]:
        r = (bgr & 0b11111) << 3
        g = ((bgr >> 5) & 0b11111) << 3
        b = ((bgr >> 10) & 0b11111) << 3

        rError = r >> 5
        gError = g >> 5
        bError = b >> 5

        return (r + rError, g + gError, b + bError)


    def rgb_to_bgr555(self, rgb: list[int]) -> int:
        r = rgb[0] >> 3
        g = (rgb[1] >> 3) << 5
        b = (rgb[2] >> 3) << 10

        return r + g + b

    
    def process_8bits_palette(self, colors: list[int]) -> list[list[QColor]]:
        result = []

        index = 0
        palette = []
        for color in colors:
            r, g, b = self.bgr555_to_rgb(color)
            palette.append(QColor(r, g, b))
            index += 1

            # Reset the palette at 16 colors
            if index == 16:
                index = 0
                result.append(palette)
                palette = []

        return result


    def process_4bit_palettes(self, colors: list[list[int]]) -> list[list[QColor]]:
        result = []

        for palette in colors:
            qpalette = []
            for color in palette:
                r, g, b = self.bgr555_to_rgb(color)
                qpalette.append(QColor(r, g, b))

            result.append(qpalette)

        return result
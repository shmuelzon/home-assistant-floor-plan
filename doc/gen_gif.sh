ffmpeg -i demo.mp4 -filter_complex "fps=5,split[v1][v2]; [v1]palettegen=stats_mode=full [palette]; [v2][palette]paletteuse=dither=sierra2_4a" -vsync 0 demo.gif

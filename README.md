# TestDemo 说明

目前通过使用 FFmpeg 滤镜的方式，将比例不是 的视频文件转化高宽比  4：3。

尝试使用 FFmpeg 命令 「-vcodec copy」期望在加滤镜的时候使用原视频，结果失败，使用此条命令的时候将会无视滤镜的效果

可能可以通过类似加水印的方式来完成需求？
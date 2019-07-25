import cv2


def getAverageRGB(path):
    list = []
    img = cv2.imread("" + path)
    bgr_channel = cv2.split(img)
    for channel in bgr_channel:
        list.append(getAverageAtChannel(channel))


def getAverageAtChannel(channel):
    count = 0
    pixel_sum = 0
    width = channel.width
    height = channel.height

    for i in range(width):
        for j in range(height):
            sum += channel[i][j]
            count += 1

    return sum / count

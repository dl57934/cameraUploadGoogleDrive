import cv2


def getAverageRGB(path):
    bgr_list = []
    img = cv2.imread(path)
    bgr_channel = cv2.split(img)
    for channel in bgr_channel:
        bgr_list.append(getAverageAtChannel(channel))
    return bgr_list


def getAverageAtChannel(channel):
    count = 0
    pixel_sum = 0
    [width, height] = channel.shape

    for i in range(width):
        for j in range(height):
            pixel_sum += channel[i][j]
            count += 1

    return pixel_sum / count

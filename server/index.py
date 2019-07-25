from flask import Flask, request
import pandas
import json
from imageProcessing import getAverageRGB
from saveCsv import saveAtCsv

app = Flask(__name__)


@app.route("/", methods=["GET", "POST"])
def root():
    return "hi flask"


@app.route("/sendImage", methods=["GET", "POST"])
def test():
    print(request.files['image'])
    # 저장되는 구간
    bgr = getAverageRGB(request.files['image'])
    data = {"blue": bgr[0], "green": bgr[1], "red": bgr[2], "location": 1, "flowerShape": 0.8}
    saveAtCsv(newData=data, path="")
    return json.dumps({"success": "hihi"})


if __name__ == '__main__':
    app.run(host='0.0.0.0')

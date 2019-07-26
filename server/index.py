from flask import Flask, request
import json
from imageProcessing import getAverageRGB
from saveCsv import saveAtCsv

app = Flask(__name__)


@app.route("/", methods=["GET", "POST"])
def root():
    return "hi flask"


@app.route("/sendImage", methods=["GET", "POST"])
def test():
    receive_file = request.files['image']
    path = "/resource/" + receive_file.filename
    receive_file.save(receive_file.filename)

    bgr = getAverageRGB(path=receive_file.filename)
    data = {"imageName":receive_file.filename,
            "blue": bgr[0], "green": bgr[1], "red": bgr[2],
            "location": 1, "flowerShape": 0.8}
    saveAtCsv(new_data=data, path="dataSet.csv")
    return json.dumps({"success": "hihi"})


if __name__ == '__main__':
    app.run(host='0.0.0.0')

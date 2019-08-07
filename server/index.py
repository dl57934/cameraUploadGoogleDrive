from flask import Flask, request
import json
from imageProcessing import getAverageRGB
from saveCsv import saveAtCsv
import os
import shutil

if not os.path.isfile("dataSet.csv"):
    f = open("dataSet.csv", 'w')
    f.write("imageName,blue,green,red,location,flowerShape,brix")
    f.close()

app = Flask(__name__)


@app.route("/", methods=["GET", "POST"])
def root():
    return "hi flask"


@app.route("/sendImage", methods=["GET", "POST"])
def test():
    receive_file = request.files['image']
    location, brix = request.args.get("location").split(",")
    receive_file.save(receive_file.filename)

    bgr = getAverageRGB(path=receive_file.filename)
    data = {"imageName": receive_file.filename,
            "blue": bgr[0], "green": bgr[1], "red": bgr[2],
            "location": location, "flowerShape": 0.8, "brix":brix}
    saveAtCsv(new_data=data, path="dataSet.csv")
    shutil.move(receive_file.filename, "resource/"+receive_file.filename)

    return json.dumps({"success": "hihi"})


if __name__ == '__main__':
    app.run(host='0.0.0.0', debug=True)

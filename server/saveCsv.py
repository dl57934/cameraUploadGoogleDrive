import pandas


def saveAtCsv(newData, path):
    csv = pandas.read_csv(path)
    following_csv = pandas.concat([csv, newData], ignore_index=True)
    following_csv.to_csv(path, index=False)


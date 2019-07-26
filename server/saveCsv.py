import pandas as pd


def saveAtCsv(new_data, path):
    csv = pd.read_csv(path)
    new_data = pd.DataFrame(new_data, index=[len(csv)+1])
    following_csv = pd.concat([csv, new_data], ignore_index=True)
    following_csv.to_csv(path, index=False)


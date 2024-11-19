import csv

def read_data(file):
    with open(file, "r") as f:
        f = list(csv.reader(f))
        f.pop(0)  # Remove the header
        return f

def take_out_data(data, output_file):
    with open(output_file, "w") as out:
        for row in data:
            if len(row[7]) > 1:
                out.write(f"{row[7]}\n")
            if len(row[8]) > 1:
                out.write(f"{row[8]}\n")
            if len(row[9]) > 1:
                out.write(f"{row[9]}\n")

if __name__ == "__main__":
    input_file = "/s/bach/g/under/tste/cs435/CS-435-Final-Project/nba/play_by_play.csv"
    output_file = "/s/bach/g/under/tste/cs435/CS-435-Final-Project/nba/output_data.txt"

    data = read_data(input_file)
    take_out_data(data, output_file)
    print(f"Data written to {output_file}")

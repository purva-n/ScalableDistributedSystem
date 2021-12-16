'''
Make sure numpy is installed (e.g. "pip install numpy" at command line).
Simply supply the path to the file to be parsed on the command line.
If you are on Windows, use \\ between directories or /:
    e.g. C:\\Users\\e.gurish\\Downloads\\test.out
    or C:/Users/e.gurish/Downloads/test.out
'''

import numpy, sys

def main():
    gets = []
    posts = []
    with open(sys.argv[1]) as file:
        line = file.readline()
        while line:
            if (line.startswith("GET")):
                gets.append(int(line[4:].strip()))
            else:
                posts.append(int(line[5:].strip()))
            line = file.readline()

    print("POST Mean: ", numpy.mean(posts))
    print("GET Mean: ", numpy.mean(gets))
    print("POST 90th Percentile: ", numpy.percentile(posts, 90))
    print("POST 99th Percentile: ", numpy.percentile(posts, 99))
    print("GET 90th Percentile: ", numpy.percentile(gets, 90))
    print("GET 99th Percentile: ", numpy.percentile(gets, 99))    

main()

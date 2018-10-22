from flask import Flask, request

app = Flask(__name__)

@app.route('/card_info', methods = ['POST'])
def info():
    rank = request.form['rank']
    suit = request.form['suit']
    card_info = rank + ' ' + suit
    print(card_info)
    return card_info

if __name__ == '__main__':
    app.run(host='0.0.0.0', debug=True, threaded=True)


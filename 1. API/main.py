from flask import Flask, request, jsonify, send_file
import os
from app.models.file_upload import FileUpload
from app.models.trainer import Trainer

app = Flask(__name__)


@app.route('/ping', methods=['GET'])
def ping():
    response = {'statusCode': 200, 'message': "ok"}
    return jsonify("response"), response['statusCode']


@app.route('/upload', methods=['POST'])
def upload():
    try:
        response = FileUpload().extract_and_upload_clips_from_http_request(request)
        return jsonify(response), response['statusCode']
    except Exception as e:
        response = {'log': [], 'statusCode': 500, 'message': f"{e}", 'data': None}
        return jsonify(response), response['statusCode']


@app.route('/uploadSingle', methods=['POST'])
def upload_single():
    try:
        response = FileUpload().upload_clips_from_http_request(request)
        return jsonify(response), response['statusCode']
    except Exception as e:
        response = {'log': [], 'statusCode': 500, 'message': f"{e}", 'data': None}
        return jsonify(response), response['statusCode']


@app.route('/process', methods=['POST'])
def process():
    try:
        response = Trainer().train_model(request)
        return jsonify(response), response['statusCode']
    except Exception as e:
        response = {'log': [], 'statusCode': 500, 'message': f"{e}", 'data': None}
        return jsonify(response), response['statusCode']


@app.route('/download', methods=['GET'])
def download():
    user_base_path = os.path.expanduser("~") + '/Soundless'
    model_path = user_base_path + '/Models/soundless_model.tflite'

    return send_file(model_path, as_attachment=True)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)

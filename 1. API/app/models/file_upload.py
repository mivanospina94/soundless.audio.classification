import os
import tempfile
import subprocess
import re
from datetime import datetime


class FileUpload:

    def __init__(self):
        print("FileUpload __init__")

    @staticmethod
    def extract_and_upload_clips_from_http_request(http_request):
        if os.path.exists(r'C:/Users/mauricio.ospina/Desktop/tmp/Personal/master/_/TFM/Train AUDIOS/ffmpeg/ffmpeg.exe'):
            ffmpeg_path = r'C:/Users/mauricio.ospina/Desktop/tmp/Personal/master/_/TFM/Train AUDIOS/ffmpeg/ffmpeg.exe'
        else:
            ffmpeg_path = r'/usr/bin/ffmpeg'

        response = {'log': []}

        # Validations
        if 'file' not in http_request.files:
            response['statusCode'] = 422
            response['message'] = "Files not found"
            response['data'] = None
            return response

        if 'labels' not in http_request.form:
            response['statusCode'] = 422
            response['message'] = "Labels not found"
            response['data'] = None
            return response

        labels = http_request.form.get('labels').split(',')

        if len(http_request.files.getlist('file')) != len(http_request.form.get('labels').split(',')):
            response['statusCode'] = 422
            response['message'] = "Num of labels do not match with num of files"
            response['data'] = None
            return response

        if 'silence_threshold' not in http_request.form:
            silence_threshold = 0.25
        else:
            silence_threshold = float(http_request.form.get('silence_threshold'))

        response['log'].append(f"Silence threshold set as '{silence_threshold}'")

        if 'minimun_silence_duration' not in http_request.form:
            min_silence_duration = 1
        else:
            min_silence_duration = float(http_request.form.get('minimun_silence_duration'))

        response['log'].append(f"Minimun silence duration set as '{min_silence_duration}'")

        if 'minimun_clip_duration' not in http_request.form:
            min_clip_duration = 5
        else:
            min_clip_duration = float(http_request.form.get('minimun_clip_duration'))

        response['log'].append(f"Minimun clip duration set as '{min_clip_duration}'")

        files = http_request.files.getlist('file')
        temp_path = tempfile.gettempdir() + '/Soundless'
        user_base_path = os.path.expanduser("~") + '/Soundless'
        user_audios_path = user_base_path + '/Audios'

        if not os.path.exists(temp_path):
            os.makedirs(temp_path)
            response['log'].append(f"Folder '{temp_path}' created.")

        if not os.path.exists(user_audios_path):
            os.makedirs(user_audios_path)
            response['log'].append(f"Folder '{user_audios_path}' created.")

        segments = []

        for idx, file in enumerate(files):
            file_name = os.path.splitext(file.filename)[0]
            file_ext = os.path.splitext(file.filename)[1]
            file_save_path = f'{temp_path}/{file.filename}'
            file_output_path = f'{user_audios_path}/{file_name}_converted{file_ext}'
            file.save(file_save_path)
            label = labels[idx]
            response['log'].append(f'File {file.filename} saved in {file_save_path}')

            # To convert and save: one channel and 16KHz
            command = [
                ffmpeg_path,
                "-y",
                "-i", file_save_path,
                "-ar", "16000",
                "-ac", "1",
                file_output_path
            ]

            try:
                subprocess.run(command, check=True)
                response['log'].append(f"Conversion of file {file.filename} completed.")
                os.remove(file_save_path)
            except subprocess.CalledProcessError as e:
                response['log'].append(f"Error when converting file {file.filename}.")
                response['log'].append(str(e))

            # Volume detect
            ffmpeg_command = [
                ffmpeg_path,
                "-y",
                "-i", file_output_path,
                "-af", "volumedetect",
                "-f", "null", "-"
            ]

            result = subprocess.run(ffmpeg_command, capture_output=True, text=True)
            output = result.stderr

            volume_data = re.findall(r"mean_volume: ([+-]?\d+\.\d+) dB", output)
            volume_data_str = volume_data[0]
            volume_data = float(volume_data_str)
            noise_threshold = ((volume_data * silence_threshold) - volume_data) * -1
            noise_threshold = format(noise_threshold, ".1f")

            response['log'].append(f"Average volume: {volume_data_str} dB")
            response['log'].append(f"Noise threshold : {noise_threshold} dB")

            # To split and save split file
            file_converted_path = file_output_path
            file_save_path = file_output_path
            file_output_path = f'{user_audios_path}/{file_name}_segment_%03d{file_ext}'

            command = [
                ffmpeg_path,
                "-y",
                "-i", file_save_path,
                "-af", "silencedetect=n=" + noise_threshold + "dB:d=" + str(min_silence_duration),
                "-f", "null", "-"
            ]

            try:
                result = subprocess.run(command, capture_output=True, text=True)
                output = result.stderr
            except subprocess.CalledProcessError as e:
                response['statusCode'] = 500
                response['message'] = "Labels not found"
                response['data'] = None
                response['log'].append(str(e))
                return response

            start_time = None
            flag = False

            for line in output.splitlines():
                if "silence_end" in line:
                    silence_end = line.split()[4]
                    start_time = float(silence_end)
                    flag = True
                elif "silence_start" in line and flag:
                    silence_start = line.split()[4]
                    end_time = float(silence_start)
                    duration = end_time - start_time

                    if duration >= min_clip_duration:
                        segments.append((start_time, end_time))

            response['log'].append("Starting splitting audios")

            for i, (start, end) in enumerate(segments, start=1):
                ffmpeg_command = [
                    ffmpeg_path,
                    "-y",
                    "-i", file_save_path
                ]
                segment_output = file_output_path % i
                ffmpeg_command.extend(["-ss", str(start), "-to", str(end), "-c:a", "copy", segment_output])
                subprocess.run(ffmpeg_command)
                with open(user_base_path + '/audios_labels.txt', 'a+') as txtFile:
                    txtFile.write(f"{file_name + file_ext}\t{segment_output}\t{label}\n")

            os.remove(file_converted_path)
            response['log'].append(f"Temporary file {file_converted_path} removed")

        response['log'].append(f"{len(segments)} audio clips generated")
        response['statusCode'] = 200
        response['message'] = 'Process executed successfully'
        response['data'] = None
        response['log'].append("Done.")

        return response

    @staticmethod
    def upload_clips_from_http_request(request):
        use_same_label = request.args.get('use_same_label', default='false').lower() == 'true'

        if os.path.exists(r'C:/Users/mauricio.ospina/Desktop/tmp/Personal/master/_/TFM/Train AUDIOS/ffmpeg/ffmpeg.exe'):
            ffmpeg_path = r'C:/Users/mauricio.ospina/Desktop/tmp/Personal/master/_/TFM/Train AUDIOS/ffmpeg/ffmpeg.exe'
        else:
            ffmpeg_path = r'/usr/bin/ffmpeg'

        response = {'log': []}

        # Validations
        if 'file' not in request.files:
            response['statusCode'] = 422
            response['message'] = "Files not found"
            response['data'] = None
            return response

        if 'labels' not in request.form:
            response['statusCode'] = 422
            response['message'] = "Labels not found"
            response['data'] = None
            return response

        labels = request.form.get('labels').split(',')

        if len(request.files.getlist('file')) != len(request.form.get('labels').split(',')) and not use_same_label:
            response['statusCode'] = 422
            response['message'] = "Num of labels do not match with num of files"
            response['data'] = None
            return response

        files = request.files.getlist('file')
        temp_path = tempfile.gettempdir() + '/Soundless'
        user_base_path = os.path.expanduser("~") + '/Soundless'
        user_audios_path = user_base_path + '/Audios'

        if not os.path.exists(temp_path):
            os.makedirs(temp_path)
            response['log'].append(f"Folder '{temp_path}' created.")

        if not os.path.exists(user_audios_path):
            os.makedirs(user_audios_path)
            response['log'].append(f"Folder '{user_audios_path}' created.")

        for idx, file in enumerate(files):
            timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
            file_name = timestamp + os.path.splitext(file.filename)[0]
            file_ext = os.path.splitext(file.filename)[1]
            file_save_path = f'{temp_path}/{file.filename}'
            file_output_path = f'{user_audios_path}/{file_name}{file_ext}'
            file.save(file_save_path)
            label = labels[idx] if not use_same_label else labels[0]
            response['log'].append(f'File {file.filename} saved in {file_save_path}')

            # To convert and save: one channel and 16KHz
            command = [
                ffmpeg_path,
                "-y",
                "-i", file_save_path,
                "-ar", "16000",
                "-ac", "1",
                file_output_path
            ]

            try:
                subprocess.run(command, check=True)
                response['log'].append(f"Conversion of file {file.filename} completed.")
                os.remove(file_save_path)
            except subprocess.CalledProcessError as e:
                response['log'].append(f"Error when converting file {file.filename}.")
                response['log'].append(str(e))

            with open(user_base_path + '/audios_labels.txt', 'a+') as txtFile:
                txtFile.write(f"{file_name + file_ext}\t{file_output_path}\t{label}\n")

        response['log'].append(f"{len(files)} audio clips generated")
        response['statusCode'] = 200
        response['message'] = 'Process executed successfully'
        response['data'] = None
        response['log'].append("Done.")

        return response

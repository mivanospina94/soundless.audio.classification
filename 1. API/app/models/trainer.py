import os
import pandas as pd
import random
import shutil
from tflite_model_maker import audio_classifier


class Trainer:

    def __init__(self):
        print("FileUpload __init__")

    @staticmethod
    def train_model(request):
        train = request.args.get('train', default='false').lower() == 'true'
        user_base_path = os.path.expanduser("~") + '/Soundless'
        user_training_path = user_base_path + '/Training'
        models_path = user_base_path + '/Models/'
        response = {'log': []}

        # read files and labels
        df_columns = ['file_basename', 'file_clipname', 'label']
        df_audios_labels = pd.read_csv(user_base_path + '/audios_labels.txt', sep='\t', header=None, names=df_columns)

        df_audios_labels['file_clipname'] = df_audios_labels.groupby('label')['file_clipname'] \
            .transform(lambda x: random.sample(x.tolist(), len(x)))
        df_audios_labels['file_basename'] = df_audios_labels.groupby('label')['file_basename'] \
            .transform(lambda x: random.sample(x.tolist(), len(x)))
        value_counts = df_audios_labels['label'].value_counts()
        df_audios_labels['frequency'] = df_audios_labels['label'].map(value_counts)
        df_audios_labels['percentage'] = (1 / df_audios_labels['frequency'])
        df_audios_labels['acum_percentage'] = df_audios_labels.groupby('label')['percentage'].cumsum()

        df_audios_labels = df_audios_labels.drop(['frequency', 'percentage'], axis=1)

        # move files to train/test folders
        if os.path.exists(user_training_path + '/train'):
            shutil.rmtree(user_training_path + '/train')
            response['log'].append("'train' folder removed.")

        if os.path.exists(user_training_path + '/test'):
            shutil.rmtree(user_training_path + '/test')
            response['log'].append("'test' folder removed.")

        if not os.path.exists(models_path):
            os.makedirs(models_path)
            response['log'].append(f"Folder '{models_path}' created.")

        os.makedirs(user_training_path + '/train')
        os.makedirs(user_training_path + '/test')

        for label in df_audios_labels['label'].unique():
            os.makedirs(user_training_path + '/train' + f'/{label}')
            response['log'].append(f"'/train/{label}' folder created.")
            os.makedirs(user_training_path + '/test' + f'/{label}')
            response['log'].append(f"'/test/{label}' folder created.")

        for index, row in df_audios_labels.iterrows():
            output_folder = user_training_path + '/train' if row[
                                                                 'acum_percentage'] < 0.8 else user_training_path + '/test'
            output_path = os.path.join(output_folder, row['label'], os.path.basename(row['file_clipname']))

            shutil.copy(row['file_clipname'], output_path)

        response['log'].append(f"Processed {len(df_audios_labels)} files")

        # Training...
        if not train:
            response['log'].append(f'Files organized in folders only, train process no executed')
            response['statusCode'] = 200
            response['message'] = "Process executed successfully"
            return response, response['statusCode']
        # test_files = os.path.abspath(os.path.join(user_training_path, 'test/*/*.wav'))
        # train_files = os.path.abspath(os.path.join(user_training_path, 'train/*/*.wav'))

        spec = audio_classifier.YamNetSpec(
            keep_yamnet_and_custom_heads=True,
            frame_step=3 * audio_classifier.YamNetSpec.EXPECTED_WAVEFORM_LENGTH,
            frame_length=6 * audio_classifier.YamNetSpec.EXPECTED_WAVEFORM_LENGTH)

        train_data = audio_classifier.DataLoader.from_folder(spec, os.path.join(user_training_path, 'train'),
                                                             cache=True)
        train_data, validation_data = train_data.split(0.8)
        test_data = audio_classifier.DataLoader.from_folder(spec, os.path.join(user_training_path, 'test'), cache=True)

        batch_size = 8  # train_data.size #TODO: Make parameter
        epochs = 100  # TODO: Make parameter

        response['log'].append('Training the model...')
        model = audio_classifier.create(
            train_data,
            spec,
            validation_data,
            batch_size=batch_size,
            epochs=epochs)

        response['log'].append('Evaluating the model...')
        model.evaluate(validation_data)

        response['log'].append(f'Exporting the TFLite model to {models_path}')

        model.export(models_path, tflite_filename='soundless_model.tflite')

        response['statusCode'] = 200
        response['message'] = "Process executed successfully"
        return response

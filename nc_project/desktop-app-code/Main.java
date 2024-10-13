import javax.sound.sampled.*;
import org.jtransforms.fft.DoubleFFT_1D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Desktop;
import java.net.URI;

public class Main {
    private static boolean recording = false; // Флаг записи
    private static ArrayList<String> bitSequence = new ArrayList<>();
    private static int bitsCount = 0;
    // Конфигурация аудио формата с частотой дискретизации 48 кГц
    private static AudioFormat getAudioFormat() {
        float sampleRate = 96000; // частота дискретизации 48 кГц
        int sampleSizeInBits = 16; // разрядность 16 бит
        int channels = 1; // моно
        boolean signed = true; // с поддержкой знака
        boolean bigEndian = false; // порядок байт (малый конец)
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    // Преобразование байтового массива в массив вещественных чисел (для FFT)
    private static double[] bytesToDoubleArray(byte[] audioData) {
        double[] doubleData = new double[audioData.length / 2]; // 16-битные сэмплы
        for (int i = 0; i < audioData.length; i += 2) {
            int sample = (audioData[i + 1] << 8) | (audioData[i] & 0xFF); // составляем 16-битное значение
            doubleData[i / 2] = sample / 32768.0; // нормализуем значение до диапазона [-1, 1]
        }
        return doubleData;
    }

    // Вычисление частоты по результатам FFT
    private static double calculateDominantFrequency(double[] audioData, float sampleRate) {
        int n = audioData.length;

        // Применяем FFT
        DoubleFFT_1D fft = new DoubleFFT_1D(n);
        double[] fftData = new double[n * 2];// для хранения комплексных значений (реальная и мнимая части)
        System.arraycopy(audioData, 0, fftData, 0, n);
        fft.realForwardFull(fftData); // Выполняем прямое преобразование Фурье

        // Поиск доминирующей частоты
        double maxAmplitude = -1;
        int dominantIndex = -1;
        for (int i = 0; i < n / 2; i++) {
            double real = fftData[2 * i];
            double imaginary = fftData[2 * i + 1];
            double magnitude = Math.sqrt(real * real + imaginary * imaginary); // амплитуда

            if (magnitude > maxAmplitude) {
                maxAmplitude = magnitude;
                dominantIndex = i;
            }
        }

        // Расчет частоты
        double dominantFrequency = dominantIndex * sampleRate / n;
        if (dominantFrequency < 17500){
            return -1;
        }
        return dominantFrequency;
    }

    // Преобразование частот в биты
    private static String mapFrequencyToBit(double frequency) {
        if (frequency >= 18850 && frequency <= 19150) {
            return "0"; // Частота от 18850 до 19150 Гц — 0
        } else if (frequency > 19150 && frequency <= 19450) {
            return "1"; // Частота от 19850 до 20150 Гц — 1
        } else if (frequency > 19450 && frequency <= 19750) {
            return "2"; // Частота от 19850 до 20150 Гц — 1
        } else if (frequency > 19750 && frequency <= 20050) {
            return "3"; // Частота от 19850 до 20150 Гц — 1
        }
        return "-"; // Если частота не попадает в заданные диапазоны
    }

    private static double[] applyHighPassFilter(double[] data, double cutoffFrequency, double samplingRate) {
        // Пример реализации фильтра по вашему выбору
        double rc = 1.0 / (2 * Math.PI * cutoffFrequency);
        double dt = 1.0 / samplingRate;
        double alpha = dt / (rc + dt);

        double[] filtered = new double[data.length];
        filtered[0] = data[0]; // Первое значение без изменений

        for (int i = 1; i < data.length; i++) {
            filtered[i] = alpha * (filtered[i - 1] + data[i] - data[i - 1]);
        }

        return filtered;
    }

    // Создание GUI
    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Аудио Анализатор");

        // Создание минималистичной кнопки
        JButton button = new JButton("▶");
        button.setPreferredSize(new Dimension(60, 30)); // Задаём размеры кнопки
        button.setBackground(new Color(50, 50, 50)); // Цвет фона (тёмный)
        button.setForeground(Color.WHITE); // Цвет текста (белый)
        button.setFocusPainted(false); // Убираем фокус
        button.setBorderPainted(false); // Убираем рамки

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!recording) {
                    button.setText("■");
                    startAudioProcessing();
                } else {
                    button.setText("▶");
                    stopAudioProcessing();
                }
            }
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(new Color(30, 30, 30)); // Тёмный фон окна
        frame.getContentPane().setLayout(new GridBagLayout()); // Центровка кнопки
        frame.getContentPane().add(button); // Добавляем кнопку на панель
        frame.setSize(300, 100);
        frame.setVisible(true);
    }

    // Запуск обработки аудио
    private static void startAudioProcessing() {
        recording = true;
        new Thread(() -> {
            mainAudioProcessing();
        }).start();
    }

    // Остановка обработки аудио
    private static void stopAudioProcessing() {
        recording = false; // Установите флаг записи в false, чтобы остановить обработку
        System.out.println("Окончание записи...");
        System.out.println("Записанная последовательность: " + bitSequence);
        System.out.println(bitsCount);
        bitSequence.clear();
    }

    private static void showDecodedResultDialog(String decoded) {
        String url = "https://" + decoded;
        String message = "Decoded string: " + decoded + "\n\nDo you want to open the event link?";

        int response = JOptionPane.showConfirmDialog(null, message, "Decoded Result", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void mainAudioProcessing() {
        AudioFormat format = getAudioFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        int sampleRate = (int) format.getSampleRate();

        // Задаём время анализа в миллисекундах напрямую в коде
        int analysisTimeMs = 100; // можно менять от 100 до 1000 мс

        // Список для хранения всех слышимых частот
        ArrayList<Double> allFrequencies = new ArrayList<>();
        boolean recording = false; // Флаг записи

        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Микрофон не поддерживается.");
            return;
        }

        try (TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info)) {
            microphone.open(format);
            microphone.start();

            System.out.println("Начинается прослушивание звука с микрофона...");

            while (true) {
                int bufferSize = (int) (sampleRate * analysisTimeMs / 1000.0); // количество сэмплов для текущего времени анализа
                byte[] buffer = new byte[bufferSize * 2]; // буфер для хранения аудиоданных (умножено на 2 из-за 16 бит)

                int bytesRead = microphone.read(buffer, 0, buffer.length); // читаем звук в буфер

                // Преобразуем аудиоданные в формат double для анализа
                double[] audioData = bytesToDoubleArray(buffer);

                audioData = applyHighPassFilter(audioData, 17000, sampleRate);

                // Вычисляем текущую частоту
                double dominantFrequency = calculateDominantFrequency(audioData, format.getSampleRate());

                // Подсчитываем, сколько раз встречалась каждая частота
                Map<Double, Integer> frequencyCount = new HashMap<>();
                double roundedFrequency = Math.round(dominantFrequency);
                frequencyCount.put(roundedFrequency, frequencyCount.getOrDefault(roundedFrequency, 0) + 1);

                // Находим наиболее часто встречающуюся частоту за последние N мс
                double mostFrequentFrequency = frequencyCount.entrySet()
                        .stream()
                        .max(Map.Entry.comparingByValue())
                        .get()
                        .getKey();

                // Если услышали 18500 Гц, то включаем запись
                if (Math.abs(mostFrequentFrequency - 18600) < 50) { // допустим небольшую погрешность ±50 Гц
                    if (!recording) {
                        System.out.println("Начало записи...");
                        recording = true;
                    }
                }

                // Если запись активна, сохраняем частоту
                if (recording) {
                    allFrequencies.add(mostFrequentFrequency);
                    //System.out.println("Частота: " + mostFrequentFrequency + " Гц");
                }

                // Если услышали 18000 Гц, то завершаем запись
                if (Math.abs(mostFrequentFrequency - 18300) < 50) { // допустим небольшую погрешность ±50 Гц
                    if (recording) {
                        System.out.println("Окончание записи...");
                        recording = false;

                        // Преобразуем частоты в биты
                        String bitSequence = "";
                        for (double frequency : allFrequencies) {
                            String bit = mapFrequencyToBit(frequency);
                            if (!bit.equals("-")) { // Добавляем только если частота в нужном диапазоне
                                bitSequence=bitSequence+bit;
                            }
                        }

                        // Выводим последовательность битов
                        System.out.println("Записанная последовательность: " + bitSequence);
                        String decoded=QuaternaryDecoding.decodeQuaternary(bitSequence);
                        System.out.println("\n декодированная строка:" + decoded.replaceFirst("/", "."));
                        showDecodedResultDialog(decoded.replaceFirst("/", "."));
                        // Очищаем массив для следующей записи
                        allFrequencies.clear();
                    }
                }

                // Задержка для следующего цикла
                Thread.sleep(1); // динамическая задержка
            }
        } catch (LineUnavailableException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }
}

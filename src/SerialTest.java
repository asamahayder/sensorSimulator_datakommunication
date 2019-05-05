import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

public class SerialTest implements SerialPortEventListener {
	SerialPort serialPort;

	/** The port we're normally going to use. */
	private static final String PORT_NAMES[] = {
		"/dev/tty.usbserial-A9007UX1", // Mac
		"/dev/ttyUSB1", // Linux
		"COM7", // Windows
	};
	private BufferedReader input;
	private OutputStream output;
	private static final int TIME_OUT = 20000;
	private static final int DATA_RATE = 115200;
	private ArrayList<Integer> offSensors = new ArrayList<Integer>();

	public void initialize() {
		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

		// First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum
					.nextElement();
			for (String portName : PORT_NAMES) {
				if (currPortId.getName().equals(portName)) {
					portId = currPortId;
					break;
				}
			}
		}
		if (portId == null) {
			System.out.println("Could not find COM port.");
			return;
		}

		try {
			serialPort = (SerialPort) portId.open(this.getClass().getName(),
					TIME_OUT);
			serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			// open the streams
			input = new BufferedReader(new InputStreamReader(
					serialPort.getInputStream()));
			// System.out.println("reader: "+input.readLine());
			output = serialPort.getOutputStream();

			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	public synchronized void close() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	public synchronized void serialEvent(SerialPortEvent oEvent) {
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				String inputLine = null;
				if (input.ready()) {
					inputLine = input.readLine();
					System.out.println("\"" + inputLine + "\"AAA");

					//if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
					if (inputLine.equals("loserville")) {
						System.out.println("Starting sensor sampling");
						//File fin = new File("/tmp/log.csv"); // a linux example
						File fin = new File("C:\\Users\\Asama\\Desktop\\sensorSimulator_datakommunication\\log.csv");
						try {
							ArrayList<String> outputList = readFile(fin);

							for (int i = 0; i < outputList.size(); i++) {
								String outputLine = outputList.get(i);
								String elements[] = outputLine.split(",");
								for (Integer offSensor : offSensors) {
									if(elements.length < offSensor)
										continue;
									elements[offSensor] = "-";
								}
								outputLine = Arrays.toString(elements).replace(" ", "").replace("[", "").replace("]", "") + "\n"; // quick and dirty, and ugly

								//System.out.println("out: " + outputLine);
								output.write(outputLine.getBytes());
								output.flush();
							}

						} catch (Exception e) {
							System.err.println(e.toString());
						}
					}
					else if(inputLine.split(" ")[0].equals("off"))
					{
						int sensor;
						String stringOut = "ERROR!\n";
						try {
							sensor = Integer.parseInt(inputLine.split(" ")[1]);
							System.out.println("disabling sensor: " + Integer.toString(sensor));
							if(!offSensors.contains(sensor)){
								offSensors.add(sensor);
								stringOut = "OK!\n";
							}
						} catch (NumberFormatException e) {
							System.out.println("Wrong usage of the off command");
						}
						output.write(stringOut.getBytes());
						output.flush();
					}
					else if(inputLine.split(" ")[0].equals("on"))
					{
						int sensor;
						String stringOut = "ERROR!\n";
						try {
							sensor = Integer.parseInt(inputLine.split(" ")[1]);
							System.out.println("renabling sensor: " + Integer.toString(sensor));
							if(offSensors.contains(sensor)) {
								offSensors.remove(new Integer(sensor));
								stringOut = "OK!\n";
							}
						} catch (NumberFormatException e) {
							System.out.println("Wrong usage of the off command");
						}
						output.write(stringOut.getBytes());
						output.flush();
					}
					else if(inputLine.split(" ")[0].equals("status"))
					{
						String stringOut = "";
						for (Integer sensor : offSensors) {
							System.out.println(Integer.toString(sensor) + " is off");
							stringOut += Integer.toString(sensor) + " off" + "\n";
						}
						stringOut += "status end\n";
						output.write(stringOut.getBytes());
					}

				}

			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}

	}

	// Ignore all the other eventTypes, but you should consider the other ones.

	private static ArrayList<String> readFile(File fin) throws IOException {
		FileInputStream fis = new FileInputStream(fin);
		ArrayList<String> fileList = new ArrayList<String>();
		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		while ((line = br.readLine()) != null) {
			// System.out.println(line);
			fileList.add(line);
		}

		// br.close();
		return fileList;
		// System.out.println(line);
	}

	public static void main(String[] args) throws Exception {
		SerialTest main = new SerialTest();
		main.initialize();
		Thread t = new Thread() {
			public void run() {
				// the following line will keep this app alive for 1000 seconds,
				// waiting for events to occur and responding to them (printing
				// incoming messages to console).
				try {
					Thread.sleep(100000000);
				} catch (InterruptedException ie) {
					System.err.println(".." + ie.getMessage());
				}
			}
		};
		t.start();
		System.out.println("Started");
	}
}
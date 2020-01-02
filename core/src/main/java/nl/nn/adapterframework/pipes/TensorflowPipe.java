package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import org.tensorflow.*;

public class TensorflowPipe extends FixedForwardPipe {
	private String modelPath, inputTensor, outputTensor;
	private String[] tags;
	private Graph graph;
	private String dataType = "double";
	private long[] shape;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		System.out.println("Tensorflow version: " + TensorFlow.version());
		try {
			SavedModelBundle savedModelBundle = SavedModelBundle.load(modelPath, "serve");
			graph = savedModelBundle.graph();
		} catch (Exception e) {
			throw new ConfigurationException("Error with configuration of Tensorflow Pipe.", e);
		}
	}

	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		try (Session tfSession = new Session(graph)) {
			// Create tensor as input
			Tensor inputData;
			try {
				 inputData = createTensor(input.toString());
			}catch (NumberFormatException e) {
				throw new PipeRunException(this, "Could not parse input into given datatype [" + dataType + "]", e);
			}
			// Calculate output
			Tensor result = tfSession.runner()
					.feed(inputTensor, inputData)
					.fetch(outputTensor)
					.run().get(0);
			String output = tensor2Csv(result);
			return new PipeRunResult(getForward(), output);
		} catch (Exception e) {
			throw new PipeRunException(this, "Exception has occurred when running the Tensorflow session.", e);
		}
	}

	private String tensor2Csv(Tensor result) {
		float[][] out = new float[(int) result.shape()[0]][(int) result.shape()[1]];
		result.copyTo(out);
		StringBuilder builder = new StringBuilder();
		for (float[] floats : out) {
			for (float aFloat : floats) builder.append(aFloat).append(",");
			builder.append("\n");
		}
		return builder.toString();
	}

	private Tensor createTensor(Object input) throws ClassNotFoundException {
		String[] array = input.toString().replaceAll("\\s+", "").split(",");

		Class<?> cls = Class.forName(dataType);
		Object[] data = new Object[array.length];

		for (int i = 0; i < array.length; i++) {
			if (cls.equals(Integer.class)) {
				data[i] = Integer.parseInt(array[i]);
			} else if (cls.equals(Long.class)) {
				data[i] = Long.parseLong(array[i]);
			} else if (cls.equals(Double.class)) {
				data[i] = Double.parseDouble(array[i]);
			} else if (cls.equals(Float.class)) {
				data[i] = Float.parseFloat(array[i]);
			}else {
				throw new IllegalArgumentException("The given data type is not valid!");
			}
		}

		return Tensor.create(data, cls);
	}

	public void setModelPath(String modelPath) {
		this.modelPath = modelPath;
	}

	public String getModelPath() {
		return modelPath;
	}

	public void setTensorflowTags(String tags) {
		this.tags = tags.split(",");
	}

	public void setTensorflowDataType(String dataType) {
		this.dataType = dataType.toLowerCase();
	}

	public void setInputShape(String shape) {
		String[] array = shape.replaceAll("\\s+", "").split(",");
		this.shape = new long[array.length];
		for (int i = 0; i < array.length; i++)
			this.shape[i] = Long.parseLong(array[i]);

	}

	public void setInputSensor(String inputSensor) {
		this.inputTensor = inputSensor;
	}

	public void setOutputTensor(String outputTensor) {
		this.outputTensor = outputTensor;
	}
}

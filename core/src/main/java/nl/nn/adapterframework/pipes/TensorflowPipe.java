package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import org.apache.commons.lang.StringUtils;
import org.tensorflow.*;

public class TensorflowPipe extends FixedForwardPipe {
	private String modelPath, inputTensor, outputTensor;
	private String[] tags = {"serve"};
	private Graph graph;
	private Class<?> inputType = Double.class;
	private Class<?> outputType = Double.class;
	private long[] shape;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		System.out.println("Tensorflow version: " + TensorFlow.version());
		try {
			SavedModelBundle savedModelBundle = SavedModelBundle.load(modelPath, tags);
			graph = savedModelBundle.graph();
		} catch (Exception e) {
			throw new ConfigurationException("Error with configuration of Tensorflow Pipe.", e);
		}
	}

	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		try (Session tfSession = new Session(graph)) {
			// Create Tensor<?> as input
			Tensor<?> inputData;
			try {
				inputData = createTensor(input.toString());
			} catch (NumberFormatException e) {
				throw new PipeRunException(this, "Could not parse input into given datatype [" + inputType + "]", e);
			}
			// Calculate output
			Session.Runner runner = tfSession.runner();
			Tensor<?> result = runner
					.feed(inputTensor, inputData)
					.fetch(outputTensor)
					.run().get(0).expect(outputType);
			String output = tensor2Csv(result);
			return new PipeRunResult(getForward(), output);
		} catch (Exception e) {
			throw new PipeRunException(this, "Exception has occurred when running the Tensorflow session.", e);
		}
	}

	private String tensor2Csv(Tensor<?> result) {
		float[][] out = new float[(int) result.shape()[0]][(int) result.shape()[1]];
		result.copyTo(out);
		StringBuilder builder = new StringBuilder();
		for (float[] floats : out) {
			for (float aFloat : floats) builder.append(aFloat).append(",");
			builder.append("\n");
		}
		return builder.toString();
	}

	private Tensor<?> createTensor(Object input) throws ClassNotFoundException {
		String[] array = input.toString().replaceAll("\\s+", "").split(",");

		if (inputType.equals(Integer.class)) {
			int[] data = new int[array.length];
			for (int i = 0; i < array.length; i++) {
				data[i] = Integer.parseInt(array[i]);
			}
			return Tensor.create(data);
		} else if (inputType.equals(Long.class)) {
			long[] data = new long[array.length];
			for (int i = 0; i < array.length; i++) {
				data[i] = Long.parseLong(array[i]);
			}
			return Tensor.create(data);
		} else if (inputType.equals(Double.class)) {
			double[] data = new double[array.length];
			for (int i = 0; i < array.length; i++) {
				data[i] = Double.parseDouble(array[i]);
			}
			return Tensor.create(data);
		} else if (inputType.equals(Float.class)) {
			float[] data = new float[array.length];
			for (int i = 0; i < array.length; i++) {
				data[i] = Float.parseFloat(array[i]);
			}
			return Tensor.create(data);
		} else {
			throw new IllegalArgumentException("The given data type is not valid!");
		}
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

	public void setInputType(String inputType) {
		try{
			this.inputType = Class.forName( "java.lang." + StringUtils.capitalize(StringUtils.lowerCase(inputType)));
		} catch (ClassNotFoundException e) {
			log.error("Given output type [" + inputType + "] was not found. Falling back to default [" + this.inputType  + "] instead.");
		}
	}

	public void setOutputType(String outputType) {
		try {
			this.outputType = Class.forName("java.lang." + StringUtils.capitalize(StringUtils.lowerCase(outputType)));
		} catch (ClassNotFoundException e) {
			log.error("Given output type [" + outputType + "] was not found. Falling back to default [" + this.outputType  + "] instead.");
		}
	}

	public void setInputShape(String shape) {
		String[] array = shape.replaceAll("\\s+", "").split(",");
		this.shape = new long[array.length];
		for (int i = 0; i < array.length; i++)
			this.shape[i] = Long.parseLong(array[i]);

	}

	public void setInputTensor(String inputSensor) {
		this.inputTensor = inputSensor;
	}

	public void setOutputTensor(String outputTensor) {
		this.outputTensor = outputTensor;
	}
}

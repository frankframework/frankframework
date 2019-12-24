package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.LogUtil;
import org.tensorflow.*;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Paths;

public class TensorflowPipe extends FixedForwardPipe {
	private String modelPath;
	private String[] tags;
	private Graph graph;
	private String dataType = "double";
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
			// Create tensor as input
			Tensor inputTensor = createTensor(input);

			// Calculate output
			Tensor result = tfSession.runner()
					.feed("input", inputTensor)
					.fetch("not_activated_output")
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

	private Tensor createTensor(Object input) {
		String[] array = input.toString().replaceAll("\\s+", "").split(",");
		if (dataType.equalsIgnoreCase("integer")) {
			IntBuffer buffer = IntBuffer.allocate(array.length);
			for (String s : array) {
				buffer.put(Integer.parseInt(s));
			}
			Tensor<Integer> t = Tensor.create(shape, buffer);
			return t;
		} else if (dataType.equalsIgnoreCase("double")) {
			DoubleBuffer buffer = DoubleBuffer.allocate(array.length);
			for (String s : array) {
				buffer.put(Double.parseDouble(s));
			}
			Tensor<Double> t = Tensor.create(shape, buffer);
			return t;
		} else if (dataType.equalsIgnoreCase("long")) {
			LongBuffer buffer = LongBuffer.allocate(array.length);
			for (String s : array) {
				buffer.put(Long.parseLong(s));
			}
			Tensor<Long> t = Tensor.create(shape, buffer);
			return t;
		} else if (dataType.equalsIgnoreCase("float")) {
			FloatBuffer buffer = FloatBuffer.allocate(array.length);
			for (String s : array) {
				buffer.put(Float.parseFloat(s));
			}
			Tensor<Float> t = Tensor.create(shape, buffer);
			return t;
		}
		throw new IllegalArgumentException("The given data type is not valid!");
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
}

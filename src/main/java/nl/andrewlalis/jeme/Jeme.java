package nl.andrewlalis.jeme;

import picocli.CommandLine;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "jeme", mixinStandardHelpOptions = true)
public class Jeme implements Callable<Integer> {
	@CommandLine.Parameters(index = "0", description = "The path to the image to use as input.")
	private Path imageFilePath;

	@CommandLine.Option(names = {"-o", "--output-file"}, description = "The file to output to. Defaults to \"out.jpeg\"", defaultValue = "out.jpeg")
	private Path outputFilePath;

	@CommandLine.Option(names = {"-l", "--label"}, description = "Key-value pair representing a named label and the text you want it to have.")
	private Map<String, String> labelsRaw;

	@CommandLine.Option(names = {"--test-labels"}, description = "If this flag is set, then some filler text will be rendered for each label.", defaultValue = "false")
	private boolean testLabels;

	@CommandLine.Option(names = {"--font-size"}, description = "The size of the label text.", defaultValue = "48.0")
	private float fontSize;

	private record ImageLabel(String name, double x, double y) {}

	@Override
	public Integer call() throws Exception {
		BufferedImage img = ImageIO.read(imageFilePath.toFile());
		var labels = getLabels(imageFilePath);
		Map<ImageLabel, String> labelValues = new HashMap<>();
		if (testLabels) {
			for (var label : labels) {
				labelValues.put(label, label.name());
			}
		} else if (labelsRaw != null && labelsRaw.size() > 0) {
			for (var label : labels) {
				if (labelsRaw.containsKey(label.name())) {
					labelValues.put(label, labelsRaw.get(label.name()));
				}
			}
		} else {
			throw new IOException("Missing labels.");
		}

		Graphics2D g2 = (Graphics2D) img.getGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		Font impactFont;
		try (var in = Jeme.class.getResourceAsStream("/nl/andrewlalis/jeme/impact.ttf")) {
			if (in == null) throw new IOException("Missing font.");
			impactFont = Font.createFont(Font.TRUETYPE_FONT, in)
					.deriveFont(fontSize);
		}
		g2.setFont(impactFont);

		for (var entry : labelValues.entrySet()) {
			var label = entry.getKey();
			var text = entry.getValue();
			renderLabel(g2, label, text, img.getWidth(), img.getHeight());
		}

		// Write to output.
		ImageWriter imageWriter = null;
		String outputFileName = outputFilePath.getFileName().toString();
		int idx = outputFileName.lastIndexOf('.');
		if (idx != -1 && outputFileName.length() > idx + 1) {
			String suffix = outputFileName.substring(idx + 1);
			var writers = ImageIO.getImageWritersBySuffix(suffix);
			if (writers.hasNext()) {
				imageWriter = writers.next();
			}
		}

		if (imageWriter != null) {
			try (var out = new FileImageOutputStream(outputFilePath.toFile())) {
				imageWriter.setOutput(out);
				imageWriter.write(img);
			}
		} else {
			throw new IOException("Invalid output image format.");
		}

		return 0;
	}

	private static void renderLabel(Graphics2D g2, ImageLabel label, String text, int width, int height) {
		String[] lines = text.split("\\n");
		var fontMetrics = g2.getFontMetrics();
		int textWidth = 0;
		for (String line : lines) {
			textWidth = Math.max(textWidth, fontMetrics.stringWidth(line));
		}
		int lineHeight = fontMetrics.getHeight();
		int textHeight = lineHeight * lines.length;

		double cx = width * label.x();
		double cy = height * label.y();

		// Debug rendering.
//		g2.setColor(Color.BLUE);
//		g2.draw(new Ellipse2D.Double(cx - 10, cy - 10, 20, 20));
//		g2.setColor(Color.RED);
//		g2.draw(new Rectangle2D.Double(cx - textWidth / 2.0, cy - textHeight / 2.0, textWidth, textHeight));

		double cursorX = cx - textWidth / 2.0;
		double cursorY = cy - textHeight / 2.0 + lineHeight;

		AffineTransform original = g2.getTransform();
		AffineTransform tx = new AffineTransform();
		tx.translate(cursorX, cursorY);
		g2.setColor(Color.BLACK);
		for (String line : lines) {
			g2.setTransform(tx);
			g2.drawString(line, 0, 0);
			tx.translate(0, lineHeight);
		}
		g2.setTransform(original);
	}

	private static Collection<ImageLabel> getLabels(Path imageFilePath) throws Exception {
		String filename = imageFilePath.getFileName().toString();
		String baseName = filename;
		int extIdx = filename.lastIndexOf('.');
		if (extIdx != -1) {
			baseName = filename.substring(0, extIdx);
		}
		Path propertiesFile = imageFilePath.resolveSibling(baseName + ".properties");
		if (Files.exists(propertiesFile)) {
			Properties props = new Properties();
			try (var in = Files.newInputStream(propertiesFile)) {
				props.load(in);
				List<ImageLabel> labels = new ArrayList<>();
				for (var entry : props.entrySet()) {
					String labelName = (String) entry.getKey();
					String labelPositionStr = (String) entry.getValue();
					String[] coords = labelPositionStr.split("\\s*,\\s*");
					labels.add(new ImageLabel(
							labelName,
							Double.parseDouble(coords[0]),
							Double.parseDouble(coords[1])
					));
				}
				return labels;
			}
		} else {
			throw new IOException("Missing " + propertiesFile);
		}
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new Jeme()).execute(args);
		System.exit(exitCode);
	}
}

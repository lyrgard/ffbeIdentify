
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

public class FfbeIdentify {

	public static final int[] X_POSITIONS = {40,119,199,278,358};
	public static final int[] Y_POSITIONS = {258,370,483};
	
	public static final int[][] coordinates = {
			{3,1},
			{4,1},
			{5,1},
			{1,2},
			{2,2},
			{3,2},
			{4,2},
			{5,2},
			{1,3},
			{2,3},
			{3,3},
			{4,3}
	};

	private static final int OFFSET_X = -39;
	private static final int OFFSET_Y = -73;

	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();
		
		int w = 75, h = 54;

		int i = 0;

		Map<String, BufferedImage> references = getReferences();
		List<ImageData> images = new ArrayList<>();

		File folder = new File("D:/dev/egqss/ffbeIdentify/pulls");
		for (final File fileEntry : folder.listFiles()) {
			boolean freeBanner = false;
	        if (!fileEntry.isDirectory() && fileEntry.getName().endsWith(".png")) {
	        	
	        	BufferedImage src = ImageIO.read(fileEntry);
	        	int numberInFile = 1;
	    		for (int[] coordinate : coordinates) {
	    			freeBanner = numberInFile == 12;
	    			int x = X_POSITIONS[coordinate[0] - 1] + OFFSET_X;
	    			int y = Y_POSITIONS[coordinate[1] - 1] + OFFSET_Y;

	    			BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    			dst.getGraphics().drawImage(src, 0, 0, w, h, x, y, x + w, y + h, null);


	    			ImageData matchFound = null;
	    			String nameFound = null;
	    			for (Entry<String, BufferedImage> reference : references.entrySet()) {
	    				double diff = getDiff(dst, reference.getValue());
	    				if (diff < 0.1) {
	    					nameFound = reference.getKey();
	    					matchFound = images.stream().filter(image -> reference.getKey().equals(image.getName())).findAny().orElse(null);
	    					break;
	    				}
	    			}
	    			if (nameFound == null && matchFound == null) {
	    				for (ImageData oldImage : images) {
	    					double diff = getDiff(dst, oldImage.getImage());
	    					if (diff < 0.07) {
	    						matchFound = oldImage;
	    						break;
	    					}
	    				}
	    			}
	    			if (matchFound != null) {
	    				matchFound.newMatch(freeBanner);
	    			} else {
	    				if (nameFound == null) {
	    					ImageIO.write(dst, "png", new File("D:/dev/egqss/ffbeIdentify/croped/" + i + ".png"));
	    				}
	    				images.add(new ImageData(dst, i, nameFound, freeBanner));
	    			}
	    			i++;
	    			numberInFile++;
	    		}
	        }
	    }
		
				
		String html = "<html><head><link rel='stylesheet' href='https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css'><script src='https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js'></script><script src='https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js'></script></head><body><div class='container'><table class='table table-striped'><thead><tr><th>Unit</th><th>Name</th><th>Rarity</th><th>Free Banner</th><th>Dragon Killers banner</th></tr></thead><tbody>";
		String csv = "Unit name;Rarity;Free Banner:Dragon Killer Banner;\n";
		for (ImageData image : images) {
			String unitName;
			String imageUrl;
			if (image.getName() == null) {
				unitName = "?";
				imageUrl = "croped/"+ image.getId();
			} else {
				unitName = image.getDisplayName();
				imageUrl = "references/"+ image.getName();
				csv += image.getDisplayName() + ";" + image.getRarity() + ";" + image.getMatchesFreeBanner() + ";" + image.getMatchesDragoonBanner() + ";\n";
			}
			html += "<tr><td><img src=\"" + imageUrl + ".png\"></img></td><td>"  + unitName + "</td><td>"  + image.getRarity() + "</td><td>" + image.getMatchesFreeBanner() + "</td><td>" + image.getMatchesDragoonBanner() + "</td></tr>\n";
		}
		html += "</tbody></table></div></body></html>";
		PrintWriter out = new PrintWriter("D:/dev/egqss/ffbeIdentify/result.html");
		
		out.println(html);
		out.close();
		PrintWriter outCsv = new PrintWriter("D:/dev/egqss/ffbeIdentify/result.csv");
		outCsv.println(csv);
		outCsv.close();
		System.out.println("Time used : " + (System.currentTimeMillis() - startTime) / 1000);
	}
	
	private static Map<String, BufferedImage> getReferences() throws IOException {
		Map<String, BufferedImage> references = new HashMap<>();

		File folder = new File("D:/dev/egqss/ffbeIdentify/references");
		for (final File fileEntry : folder.listFiles()) {
	        if (!fileEntry.isDirectory() && fileEntry.getName().endsWith(".png")) {
	        	
	        	BufferedImage img = ImageIO.read(fileEntry);
	        	references.put(fileEntry.getName().substring(0, fileEntry.getName().length() - 4), img);
	        }
		}
		return references;
	}

	private static class ImageData {
		BufferedImage image;
		long id;
		int matchesFreeBanner;
		int matchesDragoonBanner;
		String name;
		
		public ImageData(BufferedImage image, long id, String name, boolean freeBanner) {
			super();
			this.image = image;
			this.id = id;
			this.name = name;
			newMatch(freeBanner);
		}
		
		public void newMatch(boolean freeBanner) {
			if (freeBanner) {
				matchesFreeBanner++;
			} else {
				matchesDragoonBanner++;
			}
		}

		public BufferedImage getImage() {
			return image;
		}

		public int getMatchesFreeBanner() {
			return matchesFreeBanner;
		}
		

		public int getMatchesDragoonBanner() {
			return matchesDragoonBanner;
		}

		public long getId() {
			return id;
		}
		public String getName() {
			return name;
		}
		public String getDisplayName() {
			if (name != null) {
				return name.substring(2);
			} else {
				return null;
			}
		}
		public String getRarity() {
			if (name != null) {
				return name.substring(0,1) + "*";
			} else {
				return "?";
			}
		}
	}

	private static  double getDiff(BufferedImage image1, BufferedImage image2) {
		int total_no_ofPixels = 0;      
		float differenceRed, differenceGreen, differenceBlue, differenceForThisPixel;
		double nonSimilarPixels = 0l, non_Similarity = 0l;

		int startWidth = image1.getWidth() / 3;
		int stopWidth = 2 * image1.getWidth() / 3;
		int startHeight = image1.getHeight() / 2;
		int stopHeight = image1.getHeight() - 1;
		
		for (int row = startWidth; row < stopWidth; row++) {
			for (int column = startHeight; column < stopHeight; column++) {
				int image1_PixelColor, red = 0, blue = 0, green = 0;
				int image2_PixelColor, red2 = 0, blue2 = 0, green2 = 0;
				int n = 0;
				for (int x = row-1; x <= row + 1; x++) {
					for (int y = column-1; y <= column+1; y++) {
							image1_PixelColor   =  image1.getRGB(x, y);                
							red                 += (image1_PixelColor & 0x00ff0000) >> 16;
							green               += (image1_PixelColor & 0x0000ff00) >> 8;
							blue                +=  image1_PixelColor & 0x000000ff;

							image2_PixelColor   =  image2.getRGB(x, y);                
							red2                += (image2_PixelColor & 0x00ff0000) >> 16;
							green2              += (image2_PixelColor & 0x0000ff00) >> 8;
							blue2               +=  image2_PixelColor & 0x000000ff;
							n++;
					}
				}
				red /= n;
				green /= n;
				blue /= n;
				red2 /= n;
				green2 /= n;
				blue2 /= n;
				
				differenceRed   =  Math.abs(red - red2) / 255f;
				differenceGreen = Math.abs( green - green2 ) / 255f;
				differenceBlue  = Math.abs( blue - blue2 ) / 255f;
				differenceForThisPixel = ( differenceRed + differenceGreen + differenceBlue ) / 3;
				nonSimilarPixels += differenceForThisPixel;
				total_no_ofPixels++;
			}
		}
		non_Similarity = nonSimilarPixels / total_no_ofPixels;

		return non_Similarity;          
	}
}

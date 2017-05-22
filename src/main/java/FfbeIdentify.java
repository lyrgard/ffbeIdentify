
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class FfbeIdentify {

	public static int[] X_POSITIONS, Y_POSITIONS;
	public static int IMAGE_SLOT_WIDTH, IMAGE_SLOT_HEIGHT;
	private static String dataFolder ;
	
	public static List<CoordinateInformation> coordinates = new ArrayList<>();
	public static Set<String> bannerNameList = new TreeSet<>();

	public static void main(String[] args) throws IOException {

		long startTime = System.currentTimeMillis();
		
		if (args.length > 0) {
			dataFolder = args[0];
		} else {
			dataFolder = "data";
		}
		
		

		int i = 0;

		readConf();
		Map<String, BufferedImage> references = getReferences();
		List<ImageData> images = new ArrayList<>();

		File folder = new File(dataFolder + "/pulls");
		for (final File fileEntry : folder.listFiles()) {
	        if (!fileEntry.isDirectory() && fileEntry.getName().endsWith(".png")) {
	        	
	        	BufferedImage src = ImageIO.read(fileEntry);
	    		for (CoordinateInformation coordinate : coordinates) {
	    			int x = X_POSITIONS[coordinate.x - 1];
	    			int y = Y_POSITIONS[coordinate.y - 1];

	    			BufferedImage dst = new BufferedImage(IMAGE_SLOT_WIDTH, IMAGE_SLOT_HEIGHT, BufferedImage.TYPE_INT_ARGB);
	    			dst.getGraphics().drawImage(src, 0, 0, IMAGE_SLOT_WIDTH, IMAGE_SLOT_HEIGHT, x, y, x + IMAGE_SLOT_WIDTH, y + IMAGE_SLOT_HEIGHT, null);


	    			ImageData matchFound = null;
	    			String nameFound = null;
	    			for (Entry<String, BufferedImage> reference : references.entrySet()) {
	    				double diff = getDiff(dst, reference.getValue());
	    				if (diff < 0.1) {
	    					nameFound = reference.getKey();
	    					matchFound = images.stream().filter(image -> reference.getKey().equals(image.name)).findAny().orElse(null);
	    					break;
	    				}
	    			}
	    			if (nameFound == null && matchFound == null) {
	    				for (ImageData oldImage : images) {
	    					double diff = getDiff(dst, oldImage.image);
	    					if (diff < 0.07) {
	    						matchFound = oldImage;
	    						break;
	    					}
	    				}
	    			}
	    			if (matchFound == null) {
	    				if (nameFound == null) {
	    					ImageIO.write(dst, "png", new File(dataFolder + "/croped/" + i + ".png"));
	    				}
	    				matchFound = new ImageData(dst, i, nameFound);
	    				images.add(matchFound);
	    				
	    			}
	    			matchFound.newMatch(coordinate.bannerName);
	    			bannerNameList.add(coordinate.bannerName);
	    			i++;
	    		}
	        }
	    }
		
				
		String html = "<html><head><link rel='stylesheet' href='https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css'><script src='https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js'></script><script src='https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js'></script></head><body><div class='container'><table class='table table-striped'><thead><tr><th>Unit</th><th>Name</th><th>Rarity</th>";
		String csv = "Unit name;Rarity;";
		for (String bannerName : bannerNameList) {
			html += "<th>"  + bannerName + "</th>";
			csv += bannerName + ";";
		}
		html += "</tr></thead><tbody>";
		csv += "\n";
		for (ImageData image : images) {
			String unitName;
			String imageUrl;
			
			if (image.name == null) {
				unitName = "?";
				imageUrl = "croped/"+ image.id;
			} else {
				unitName = image.getDisplayName();
				imageUrl = "references/"+ image.name;
				csv += image.getDisplayName() + ";" + image.getRarity() + ";";
				for (String bannerName : bannerNameList) {
					Integer bannerMatches = image.matchesByBanner.get(bannerName);
					if (bannerMatches == null) {
						bannerMatches = 0;
					}
					csv +=  bannerMatches +  ";";
				}
				csv += "\n";
			}
			html += "<tr><td><img src=\"" + imageUrl + ".png\"></img></td><td>"  + unitName + "</td><td>"  + image.getRarity() + "</td>";
			for (String bannerName : bannerNameList) {
				Integer bannerMatches = image.matchesByBanner.get(bannerName);
				if (bannerMatches == null) {
					bannerMatches = 0;
				}
				html += "<td>" + bannerMatches + "</td>";
			}
			html += "</tr>\n"; 
		}
		html += "</tbody></table></div></body></html>";
		PrintWriter out = new PrintWriter(dataFolder + "/result.html");
		
		out.println(html);
		out.close();
		PrintWriter outCsv = new PrintWriter(dataFolder + "/result.csv");
		outCsv.println(csv);
		outCsv.close();
		System.out.println("Time used : " + (System.currentTimeMillis() - startTime) / 1000);
	}
	
	private static void readConf() throws FileNotFoundException {
        JsonObject json = Json.createReader(new FileInputStream(dataFolder + "/conf/conf.json")).readObject();
        IMAGE_SLOT_WIDTH = json.getInt("unitSlotWidth");
        IMAGE_SLOT_HEIGHT = json.getInt("unitSlotHeight");

        JsonArray array = json.getJsonArray("unitSlotPositionsX");
        X_POSITIONS = new int[array.size()];
        for (int i = 0; i < array.size(); ++i) {
        	X_POSITIONS[i] = array.getInt(i);
        }
        array = json.getJsonArray("unitSlotPositionsY");
        Y_POSITIONS = new int[array.size()];
        for (int i = 0; i < array.size(); ++i) {
        	Y_POSITIONS[i] = array.getInt(i);
        }

        array = json.getJsonArray("unitSlotInformations");
        for (int i = 0; i < array.size(); ++i) {
        	JsonObject coordinate = array.getJsonObject(i);
        	coordinates.add(new CoordinateInformation(coordinate.getInt("x"), coordinate.getInt("y"), coordinate.getString("bannerName")));
        }
	}

	private static Map<String, BufferedImage> getReferences() throws IOException {
		Map<String, BufferedImage> references = new HashMap<>();

		File folder = new File(dataFolder + "/references");
		for (final File fileEntry : folder.listFiles()) {
	        if (!fileEntry.isDirectory() && fileEntry.getName().endsWith(".png")) {
	        	
	        	BufferedImage img = ImageIO.read(fileEntry);
	        	references.put(fileEntry.getName().substring(0, fileEntry.getName().length() - 4), img);
	        }
		}
		return references;
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
	
	private static class CoordinateInformation {
		int x;
		int y;
		String bannerName;
		
		public CoordinateInformation(int x, int y, String bannerName) {
			super();
			this.x = x;
			this.y = y;
			this.bannerName = bannerName;
		}
	}
	
	private static class ImageData {
		BufferedImage image;
		long id;
		Map<String, Integer> matchesByBanner = new HashMap<>();
		String name;
		
		public ImageData(BufferedImage image, long id, String name) {
			super();
			this.image = image;
			this.id = id;
			this.name = name;
		}
		
		public void newMatch(String banner) {
			Integer matches = matchesByBanner.get(banner);
			if (matches == null) {
				matches = 0;
			}
			matchesByBanner.put(banner, matches + 1);
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
}

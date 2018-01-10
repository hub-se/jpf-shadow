package commcare.android;

import gov.nasa.jpf.vm.Verify;

/*
 * TODO: add copyright information
 */

public class MediaUtil {
	public int change(int oldVal, int newVal){return oldVal;}
	public boolean change(boolean oldVal, boolean newVal){return oldVal;}

	/**
     * @return A bitmap representation of the given image file, scaled down to the smallest
     * size that still fills the container
     *
     * More precisely, preserves the following 2 conditions:
     * 1. The larger of the 2 sides takes on the size of the corresponding container dimension
     * (e.g. if its width is larger than its height, then the new width should = containerWidth)
     * 2. The aspect ratio of the original image is maintained (so the height would get scaled
     * down proportionally with the width)
     */
	//private static Bitmap getBitmapScaledToContainer(String imageFilepath, int containerHeight,
    //int containerWidth)
	public int getBitmapScaledToContainer(int imageHeight, int imageWidth, int containerHeight, int containerWidth){
		// Determine dimensions of original image
		/*
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFilepath, o);
        int imageHeight = o.outHeight;
        int imageWidth = o.outWidth;
        */
		
		// Get a scale-down factor -- Powers of 2 work faster according to the docs, but we're
		// just doing closest size that still fills the screen
		//int heightScale = Math.round((float) imageHeight / containerHeight);
		//int widthScale = Math.round((float) imageWidth / containerWidth);
		//int scale = Math.max(widthScale, heightScale);
		Verify.ignoreIf(imageHeight <= 0 || imageWidth <= 0 || containerHeight <= 0 || containerWidth <= 0);
		
		//Old code, no shared variables so no need for execute(OLD) annotation
		int heightScale, widthScale, scale;
		heightScale = imageHeight / containerHeight;
		widthScale = imageWidth / containerWidth;
		scale = max(heightScale, widthScale);
		if (scale == 0) {
			// Rounding could possibly have resulted in a scale factor of 0, which is invalid
			// In this modified example this could have resulted by integer division 
			 scale = 1;
		 }
		
		//performSafeScaleDown(imageFilepath, scale, 0);
		int oldScaledHeight = imageHeight / scale;
		int newScaledHeight =  getBitMapScaledDownExact(imageHeight, imageWidth,
			  	-1,-1,containerHeight, containerWidth);
		return change(oldScaledHeight, newScaledHeight); 
	}
	
	private int max(int a, int b){
		if (a>=b){
			return a;
		}
		else{
			return b;
		}
	}
	
	private double min(double a, double b){
		if (a<=b){
			return a;
		}
		else{
			return b;
		}
	}
	
	private int min(int a, int b){
		if (a<=b){
			return a;
		}
		else{
			return b;
		}
	}

	
	/**
     * @return A bitmap representation of the given image file, scaled down such that the new
     * dimensions of the image are the SMALLER of the following 2 options:
     * 1) targetHeight and targetWidth
     * 2) the largest dimensions for which the original aspect ratio is maintained, without
     * exceeding either boundingWidth or boundingHeight
     *
     * Provides for the possibility that there is no target height or target width (indicated by
     * setting them to -1), in which case the 2nd option above is used.
     */
	//this modified version simply returns the new height
    //private static Bitmap getBitmapScaledDownExact(String imageFilepath,
                                                   //int originalHeight, int originalWidth,
                                                   //int targetHeight, int targetWidth,
                                                   //int boundingHeight, int boundingWidth) {
	private int getBitMapScaledDownExact(int originalHeight, int originalWidth,
												   int targetHeight,int targetWidth,
												   int boundingHeight, int boundingWidth){

        Pair<Integer, Integer> dimensImposedByContainer = getProportionalDimensForContainer(
                originalHeight, originalWidth, boundingHeight, boundingWidth);

        int newWidth, newHeight;
        if (targetHeight == -1 || targetWidth == -1) {
            newWidth = dimensImposedByContainer._1;
            newHeight = dimensImposedByContainer._2;
        } else {
            newWidth = min(dimensImposedByContainer._1, targetWidth);
            newHeight = min(dimensImposedByContainer._2, targetHeight);
        	newHeight = -1;
        }
        /*
        int approximateScaleFactor = originalWidth / newWidth;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = approximateScaleFactor;
            // Decode the bitmap with the largest integer scale down factor that will not make it
            // smaller than the final desired size
            Bitmap originalBitmap = BitmapFactory.decodeFile(imageFilepath, o);
            // From that, generate a bitmap scaled to the exact right dimensions
            return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false);
        }
        catch (OutOfMemoryError e) {
            // OOM encountered trying to decode the bitmap, so we know we need to scale down by
            // a larger factor
            return performSafeScaleDown(imageFilepath, approximateScaleFactor + 1, 0);
        }
        */
        return newHeight;
    }
	
	/**
     * @return A (width, height) pair representing the largest dimensions for which the aspect
     * ratio given by originalHeight and originalWidth is maintained, without exceeding
     * boundingHeight or boundingWidth
     */
    private Pair<Integer, Integer> getProportionalDimensForContainer(int originalHeight,
                                                                            int originalWidth,
                                                                            int boundingHeight,
                                                                            int boundingWidth) {
    	
        double heightScaleFactor = ((double)boundingHeight) / originalHeight;
        double widthScaleFactor =  ((double)boundingWidth) / originalWidth;
        double dominantScaleFactor = min(heightScaleFactor, widthScaleFactor);
    	
        //int widthImposedByContainer = (int)Math.round(originalWidth * dominantScaleFactor);
        //int heightImposedByContainer = (int)Math.round(originalHeight * dominantScaleFactor);
        int widthImposedByContainer =  (int)(originalWidth * dominantScaleFactor);
        int heightImposedByContainer = (int)(originalHeight * dominantScaleFactor);
        
        return new Pair<>(widthImposedByContainer, heightImposedByContainer);
                                                                                   
    }
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int x = (new MediaUtil()).getBitmapScaledToContainer(12,13,14,15);
	}

}

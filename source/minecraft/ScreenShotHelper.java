package net.minecraft.util;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.IntBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.event.ClickEvent;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class ScreenShotHelper
{
	private static String upimg = "http://upimg.me";
	private static String upimg(File file) throws IOException, URISyntaxException {
		URL url = new URL("http://upimg.me/upload");
	    HttpClientBuilder client = HttpClientBuilder.create();
	    HttpPost httpPost = new HttpPost(url.toURI()); //The POST request to send
	    
	    FileBody fileB = new FileBody(file, ContentType.create("image/png"), "screenshot.png");
	    
	    MultipartEntityBuilder request = MultipartEntityBuilder.create(); //The HTTP entity which will holds the different body parts, here the file
	    request.addPart("file", fileB);

	    httpPost.setEntity(request.build());
	    HttpResponse response = client.build().execute(httpPost); //Once the upload is complete (successful or not), the client will return a response given by the server
	    
	    return upimg + response.getFirstHeader("Location").getValue();
	}
    private static final Logger logger = LogManager.getLogger();
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    /** A buffer to hold pixel values returned by OpenGL. */
    private static IntBuffer pixelBuffer;

    /**
     * The built-up array that contains all the pixel values returned by OpenGL.
     */
    private static int[] pixelValues;
    private static final String __OBFID = "CL_00000656";

    /**
     * Saves a screenshot in the game directory with a time-stamped filename.  Args: gameDirectory,
     * requestedWidthInPixels, requestedHeightInPixels, frameBuffer
     */
    public static IChatComponent saveScreenshot(File p_148260_0_, int p_148260_1_, int p_148260_2_, Framebuffer p_148260_3_)
    {
        return saveScreenshot(p_148260_0_, (String)null, p_148260_1_, p_148260_2_, p_148260_3_);
    }

    /**
     * Saves a screenshot in the game directory with the given file name (or null to generate a time-stamped name).
     * Args: gameDirectory, fileName, requestedWidthInPixels, requestedHeightInPixels, frameBuffer
     */
    public static IChatComponent saveScreenshot(File p_148259_0_, String p_148259_1_, int p_148259_2_, int p_148259_3_, Framebuffer p_148259_4_)
    {
        try
        {
            File var5 = new File(p_148259_0_, "screenshots");
            var5.mkdir();

            if (OpenGlHelper.isFramebufferEnabled())
            {
                p_148259_2_ = p_148259_4_.framebufferTextureWidth;
                p_148259_3_ = p_148259_4_.framebufferTextureHeight;
            }

            int var6 = p_148259_2_ * p_148259_3_;

            if (pixelBuffer == null || pixelBuffer.capacity() < var6)
            {
                pixelBuffer = BufferUtils.createIntBuffer(var6);
                pixelValues = new int[var6];
            }

            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            pixelBuffer.clear();

            if (OpenGlHelper.isFramebufferEnabled())
            {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, p_148259_4_.framebufferTexture);
                GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
            }
            else
            {
                GL11.glReadPixels(0, 0, p_148259_2_, p_148259_3_, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
            }

            pixelBuffer.get(pixelValues);
            TextureUtil.func_147953_a(pixelValues, p_148259_2_, p_148259_3_);
            BufferedImage var7 = null;

            if (OpenGlHelper.isFramebufferEnabled())
            {
                var7 = new BufferedImage(p_148259_4_.framebufferWidth, p_148259_4_.framebufferHeight, 1);
                int var8 = p_148259_4_.framebufferTextureHeight - p_148259_4_.framebufferHeight;

                for (int var9 = var8; var9 < p_148259_4_.framebufferTextureHeight; ++var9)
                {
                    for (int var10 = 0; var10 < p_148259_4_.framebufferWidth; ++var10)
                    {
                        var7.setRGB(var10, var9 - var8, pixelValues[var9 * p_148259_4_.framebufferTextureWidth + var10]);
                    }
                }
            }
            else
            {
                var7 = new BufferedImage(p_148259_2_, p_148259_3_, 1);
                var7.setRGB(0, 0, p_148259_2_, p_148259_3_, pixelValues, 0, p_148259_2_);
            }

            File var12;

            if (p_148259_1_ == null)
            {
                var12 = getTimestampedPNGFileForDirectory(var5);
            }
            else
            {
                var12 = new File(var5, p_148259_1_);
            }

            ImageIO.write(var7, "png", var12);
            String var14 = upimg(var12);
            ChatComponentText var13 = new ChatComponentText(var14);
            var13.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, var14));
            var13.getChatStyle().setUnderlined(Boolean.valueOf(true));
            return new ChatComponentTranslation("screenshot.success", new Object[] {var13});
        }
        catch (Exception var11)
        {
            logger.warn("Couldn\'t save screenshot", var11);
            return new ChatComponentTranslation("screenshot.failure", new Object[] {var11.getMessage()});
        }
    }

    /**
     * Creates a unique PNG file in the given directory named by a timestamp.  Handles cases where the timestamp alone
     * is not enough to create a uniquely named file, though it still might suffer from an unlikely race condition where
     * the filename was unique when this method was called, but another process or thread created a file at the same
     * path immediately after this method returned.
     */
    private static File getTimestampedPNGFileForDirectory(File par0File)
    {
        String var2 = dateFormat.format(new Date()).toString();
        int var3 = 1;

        while (true)
        {
            File var1 = new File(par0File, var2 + (var3 == 1 ? "" : "_" + var3) + ".png");

            if (!var1.exists())
            {
                return var1;
            }

            ++var3;
        }
    }
}

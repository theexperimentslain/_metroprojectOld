package net.minecraft.src;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import net.minecraft.block.Block;
import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.resources.ResourcePackRepository.Entry;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.init.Blocks;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.Util.EnumOS;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.glu.GLU;
import shadersmod.client.Shaders;

public class Config {
      public static final String OF_NAME = "OptiFine";
      public static final String MC_VERSION = "1.7.10";
      public static final String OF_EDITION = "HD_U";
      public static final String OF_RELEASE = "E7";
      public static final String VERSION = "OptiFine_1.7.10_HD_U_E7";
      private static String newRelease = null;
      private static boolean notify64BitJava = false;
      public static String openGlVersion = null;
      public static String openGlRenderer = null;
      public static String openGlVendor = null;
      public static String[] openGlExtensions = null;
      public static GlVersion glVersion = null;
      public static GlVersion glslVersion = null;
      public static int minecraftVersionInt = -1;
      public static boolean fancyFogAvailable = false;
      public static boolean occlusionAvailable = false;
      private static GameSettings gameSettings = null;
      private static Minecraft minecraft = Minecraft.getMinecraft();
      private static boolean initialized = false;
      private static Thread minecraftThread = null;
      private static DisplayMode desktopDisplayMode = null;
      private static DisplayMode[] displayModes = null;
      private static int antialiasingLevel = 0;
      private static int availableProcessors = 0;
      public static boolean zoomMode = false;
      private static int texturePackClouds = 0;
      public static boolean waterOpacityChanged = false;
      private static boolean fullscreenModeChecked = false;
      private static boolean desktopModeChecked = false;
      private static DefaultResourcePack defaultResourcePackLazy = null;
      public static final Float DEF_ALPHA_FUNC_LEVEL = 0.1F;
      private static final Logger LOGGER = LogManager.getLogger();
      public static float renderPartialTicks;

      private Config() {
      }

      public static String getVersion() {
            return "OptiFine_1.7.10_HD_U_E7";
      }

      public static String getVersionDebug() {
            StringBuffer sb = new StringBuffer(32);
            if (isDynamicLights()) {
                  sb.append("DL: ");
                  sb.append(String.valueOf(DynamicLights.getCount()));
                  sb.append(", ");
            }

            sb.append("OptiFine_1.7.10_HD_U_E7");
            String shaderPack = Shaders.getShaderPackName();
            if (shaderPack != null) {
                  sb.append(", ");
                  sb.append(shaderPack);
            }

            return sb.toString();
      }

      public static void initGameSettings(GameSettings settings) {
            if (gameSettings == null) {
                  gameSettings = settings;
                  desktopDisplayMode = Display.getDesktopDisplayMode();
                  updateAvailableProcessors();
            }
      }

      public static void initDisplay() {
            checkInitialized();
            antialiasingLevel = gameSettings.ofAaLevel;
            checkDisplaySettings();
            checkDisplayMode();
            minecraftThread = Thread.currentThread();
            updateThreadPriorities();
            Shaders.startup(Minecraft.getMinecraft());
      }

      public static void checkInitialized() {
            if (!initialized) {
                  if (Display.isCreated()) {
                        initialized = true;
                        checkOpenGlCaps();
                        startVersionCheckThread();
                  }
            }
      }

      private static void checkOpenGlCaps() {
            log("");
            log(getVersion());
            log("Build: " + getBuild());
            log("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") version " + System.getProperty("os.version"));
            log("Java: " + System.getProperty("java.version") + ", " + System.getProperty("java.vendor"));
            log("VM: " + System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.info") + "), " + System.getProperty("java.vm.vendor"));
            log("LWJGL: " + Sys.getVersion());
            openGlVersion = GL11.glGetString(7938);
            openGlRenderer = GL11.glGetString(7937);
            openGlVendor = GL11.glGetString(7936);
            log("OpenGL: " + openGlRenderer + ", version " + openGlVersion + ", " + openGlVendor);
            log("OpenGL Version: " + getOpenGlVersionString());
            if (!GLContext.getCapabilities().OpenGL12) {
                  log("OpenGL Mipmap levels: Not available (GL12.GL_TEXTURE_MAX_LEVEL)");
            }

            fancyFogAvailable = GLContext.getCapabilities().GL_NV_fog_distance;
            if (!fancyFogAvailable) {
                  log("OpenGL Fancy fog: Not available (GL_NV_fog_distance)");
            }

            occlusionAvailable = GLContext.getCapabilities().GL_ARB_occlusion_query;
            if (!occlusionAvailable) {
                  log("OpenGL Occlussion culling: Not available (GL_ARB_occlusion_query)");
            }

            int maxTexSize = TextureUtils.getGLMaximumTextureSize();
            dbg("Maximum texture size: " + maxTexSize + "x" + maxTexSize);
      }

      private static String getBuild() {
            try {
                  InputStream in = Config.class.getResourceAsStream("/buildof.txt");
                  if (in == null) {
                        return null;
                  } else {
                        String build = readLines(in)[0];
                        return build;
                  }
            } catch (Exception var2) {
                  warn("" + var2.getClass().getName() + ": " + var2.getMessage());
                  return null;
            }
      }

      public static boolean isFancyFogAvailable() {
            return fancyFogAvailable;
      }

      public static boolean isOcclusionAvailable() {
            return occlusionAvailable;
      }

      public static int getMinecraftVersionInt() {
            if (minecraftVersionInt < 0) {
                  String[] verStrs = tokenize("1.7.10", ".");
                  int ver = 0;
                  if (verStrs.length > 0) {
                        ver += 10000 * parseInt(verStrs[0], 0);
                  }

                  if (verStrs.length > 1) {
                        ver += 100 * parseInt(verStrs[1], 0);
                  }

                  if (verStrs.length > 2) {
                        ver += 1 * parseInt(verStrs[2], 0);
                  }

                  minecraftVersionInt = ver;
            }

            return minecraftVersionInt;
      }

      public static String getOpenGlVersionString() {
            GlVersion ver = getGlVersion();
            String verStr = "" + ver.getMajor() + "." + ver.getMinor() + "." + ver.getRelease();
            return verStr;
      }

      private static GlVersion getGlVersionLwjgl() {
            if (GLContext.getCapabilities().OpenGL44) {
                  return new GlVersion(4, 4);
            } else if (GLContext.getCapabilities().OpenGL43) {
                  return new GlVersion(4, 3);
            } else if (GLContext.getCapabilities().OpenGL42) {
                  return new GlVersion(4, 2);
            } else if (GLContext.getCapabilities().OpenGL41) {
                  return new GlVersion(4, 1);
            } else if (GLContext.getCapabilities().OpenGL40) {
                  return new GlVersion(4, 0);
            } else if (GLContext.getCapabilities().OpenGL33) {
                  return new GlVersion(3, 3);
            } else if (GLContext.getCapabilities().OpenGL32) {
                  return new GlVersion(3, 2);
            } else if (GLContext.getCapabilities().OpenGL31) {
                  return new GlVersion(3, 1);
            } else if (GLContext.getCapabilities().OpenGL30) {
                  return new GlVersion(3, 0);
            } else if (GLContext.getCapabilities().OpenGL21) {
                  return new GlVersion(2, 1);
            } else if (GLContext.getCapabilities().OpenGL20) {
                  return new GlVersion(2, 0);
            } else if (GLContext.getCapabilities().OpenGL15) {
                  return new GlVersion(1, 5);
            } else if (GLContext.getCapabilities().OpenGL14) {
                  return new GlVersion(1, 4);
            } else if (GLContext.getCapabilities().OpenGL13) {
                  return new GlVersion(1, 3);
            } else if (GLContext.getCapabilities().OpenGL12) {
                  return new GlVersion(1, 2);
            } else {
                  return GLContext.getCapabilities().OpenGL11 ? new GlVersion(1, 1) : new GlVersion(1, 0);
            }
      }

      public static GlVersion getGlVersion() {
            if (glVersion == null) {
                  String verStr = GL11.glGetString(7938);
                  glVersion = parseGlVersion(verStr, (GlVersion)null);
                  if (glVersion == null) {
                        glVersion = getGlVersionLwjgl();
                  }

                  if (glVersion == null) {
                        glVersion = new GlVersion(1, 0);
                  }
            }

            return glVersion;
      }

      public static GlVersion getGlslVersion() {
            if (glslVersion == null) {
                  String verStr = GL11.glGetString(35724);
                  glslVersion = parseGlVersion(verStr, (GlVersion)null);
                  if (glslVersion == null) {
                        glslVersion = new GlVersion(1, 10);
                  }
            }

            return glslVersion;
      }

      public static GlVersion parseGlVersion(String versionString, GlVersion def) {
            try {
                  if (versionString == null) {
                        return def;
                  } else {
                        Pattern REGEXP_VERSION = Pattern.compile("([0-9]+)\\.([0-9]+)(\\.([0-9]+))?(.+)?");
                        Matcher matcher = REGEXP_VERSION.matcher(versionString);
                        if (!matcher.matches()) {
                              return def;
                        } else {
                              int major = Integer.parseInt(matcher.group(1));
                              int minor = Integer.parseInt(matcher.group(2));
                              int release = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 0;
                              String suffix = matcher.group(5);
                              return new GlVersion(major, minor, release, suffix);
                        }
                  }
            } catch (Exception var8) {
                  var8.printStackTrace();
                  return def;
            }
      }

      public static String[] getOpenGlExtensions() {
            if (openGlExtensions == null) {
                  openGlExtensions = detectOpenGlExtensions();
            }

            return openGlExtensions;
      }

      private static String[] detectOpenGlExtensions() {
            try {
                  GlVersion ver = getGlVersion();
                  if (ver.getMajor() >= 3) {
                        int countExt = GL11.glGetInteger(33309);
                        if (countExt > 0) {
                              String[] exts = new String[countExt];

                              for(int i = 0; i < countExt; ++i) {
                                    exts[i] = GL30.glGetStringi(7939, i);
                              }

                              return exts;
                        }
                  }
            } catch (Exception var5) {
                  var5.printStackTrace();
            }

            try {
                  String extStr = GL11.glGetString(7939);
                  String[] exts = extStr.split(" ");
                  return exts;
            } catch (Exception var4) {
                  var4.printStackTrace();
                  return new String[0];
            }
      }

      public static void updateThreadPriorities() {
            updateAvailableProcessors();
            boolean ELEVATED_PRIORITY = true;
            if (isSingleProcessor()) {
                  if (isSmoothWorld()) {
                        minecraftThread.setPriority(10);
                        setThreadPriority("Server thread", 1);
                  } else {
                        minecraftThread.setPriority(5);
                        setThreadPriority("Server thread", 5);
                  }
            } else {
                  minecraftThread.setPriority(10);
                  setThreadPriority("Server thread", 5);
            }

      }

      private static void setThreadPriority(String prefix, int priority) {
            try {
                  ThreadGroup tg = Thread.currentThread().getThreadGroup();
                  if (tg == null) {
                        return;
                  }

                  int num = (tg.activeCount() + 10) * 2;
                  Thread[] ts = new Thread[num];
                  tg.enumerate(ts, false);

                  for(int i = 0; i < ts.length; ++i) {
                        Thread t = ts[i];
                        if (t != null && t.getName().startsWith(prefix)) {
                              t.setPriority(priority);
                        }
                  }
            } catch (Throwable var7) {
                  warn(var7.getClass().getName() + ": " + var7.getMessage());
            }

      }

      public static boolean isMinecraftThread() {
            return Thread.currentThread() == minecraftThread;
      }

      private static void startVersionCheckThread() {
            VersionCheckThread vct = new VersionCheckThread();
            vct.start();
      }

      public static boolean isMipmaps() {
            return gameSettings.mipmapLevels > 0;
      }

      public static int getMipmapLevels() {
            return gameSettings.mipmapLevels;
      }

      public static int getMipmapType() {
            switch(gameSettings.ofMipmapType) {
            case 0:
                  return 9986;
            case 1:
                  return 9986;
            case 2:
                  if (isMultiTexture()) {
                        return 9985;
                  }

                  return 9986;
            case 3:
                  if (isMultiTexture()) {
                        return 9987;
                  }

                  return 9986;
            default:
                  return 9986;
            }
      }

      public static boolean isUseAlphaFunc() {
            float alphaFuncLevel = getAlphaFuncLevel();
            return alphaFuncLevel > DEF_ALPHA_FUNC_LEVEL + 1.0E-5F;
      }

      public static float getAlphaFuncLevel() {
            return DEF_ALPHA_FUNC_LEVEL;
      }

      public static boolean isFogFancy() {
            if (!isFancyFogAvailable()) {
                  return false;
            } else {
                  return gameSettings.ofFogType == 2;
            }
      }

      public static boolean isFogFast() {
            return gameSettings.ofFogType == 1;
      }

      public static boolean isFogOff() {
            return gameSettings.ofFogType == 3;
      }

      public static float getFogStart() {
            return gameSettings.ofFogStart;
      }

      public static boolean isOcclusionEnabled() {
            return gameSettings.advancedOpengl;
      }

      public static boolean isOcclusionFancy() {
            return !isOcclusionEnabled() ? false : gameSettings.ofOcclusionFancy;
      }

      public static boolean isLoadChunksFar() {
            return gameSettings.ofLoadFar;
      }

      public static int getPreloadedChunks() {
            return gameSettings.ofPreloadedChunks;
      }

      public static void dbg(String s) {
            LOGGER.info("[OptiFine] " + s);
      }

      public static void warn(String s) {
            LOGGER.warn("[OptiFine] " + s);
      }

      public static void error(String s) {
            LOGGER.error("[OptiFine] " + s);
      }

      public static void log(String s) {
            dbg(s);
      }

      public static int getUpdatesPerFrame() {
            return gameSettings.ofChunkUpdates;
      }

      public static boolean isDynamicUpdates() {
            return gameSettings.ofChunkUpdatesDynamic;
      }

      public static boolean isRainFancy() {
            if (gameSettings.ofRain == 0) {
                  return gameSettings.fancyGraphics;
            } else {
                  return gameSettings.ofRain == 2;
            }
      }

      public static boolean isWaterFancy() {
            if (gameSettings.ofWater == 0) {
                  return gameSettings.fancyGraphics;
            } else {
                  return gameSettings.ofWater == 2;
            }
      }

      public static boolean isRainOff() {
            return gameSettings.ofRain == 3;
      }

      public static boolean isCloudsFancy() {
            if (gameSettings.ofClouds != 0) {
                  return gameSettings.ofClouds == 2;
            } else if (isShaders() && !Shaders.shaderPackClouds.isDefault()) {
                  return Shaders.shaderPackClouds.isFancy();
            } else if (texturePackClouds != 0) {
                  return texturePackClouds == 2;
            } else {
                  return gameSettings.fancyGraphics;
            }
      }

      public static boolean isCloudsOff() {
            if (gameSettings.ofClouds != 0) {
                  return gameSettings.ofClouds == 3;
            } else if (isShaders() && !Shaders.shaderPackClouds.isDefault()) {
                  return Shaders.shaderPackClouds.isOff();
            } else if (texturePackClouds != 0) {
                  return texturePackClouds == 3;
            } else {
                  return false;
            }
      }

      public static void updateTexturePackClouds() {
            texturePackClouds = 0;
            IResourceManager rm = getResourceManager();
            if (rm != null) {
                  try {
                        InputStream in = rm.getResource(new ResourceLocation("mcpatcher/color.properties")).getInputStream();
                        if (in == null) {
                              return;
                        }

                        Properties props = new Properties();
                        props.load(in);
                        in.close();
                        String cloudStr = props.getProperty("clouds");
                        if (cloudStr == null) {
                              return;
                        }

                        dbg("Texture pack clouds: " + cloudStr);
                        cloudStr = cloudStr.toLowerCase();
                        if (cloudStr.equals("fast")) {
                              texturePackClouds = 1;
                        }

                        if (cloudStr.equals("fancy")) {
                              texturePackClouds = 2;
                        }

                        if (cloudStr.equals("off")) {
                              texturePackClouds = 3;
                        }
                  } catch (Exception var4) {
                  }

            }
      }

      public static boolean isTreesFancy() {
            if (gameSettings.ofTrees == 0) {
                  return gameSettings.fancyGraphics;
            } else {
                  return gameSettings.ofTrees == 2;
            }
      }

      public static boolean isGrassFancy() {
            if (gameSettings.ofGrass == 0) {
                  return gameSettings.fancyGraphics;
            } else {
                  return gameSettings.ofGrass == 2;
            }
      }

      public static boolean isDroppedItemsFancy() {
            if (gameSettings.ofDroppedItems == 0) {
                  return gameSettings.fancyGraphics;
            } else {
                  return gameSettings.ofDroppedItems == 2;
            }
      }

      public static int limit(int val, int min, int max) {
            if (val < min) {
                  return min;
            } else {
                  return val > max ? max : val;
            }
      }

      public static float limit(float val, float min, float max) {
            if (val < min) {
                  return min;
            } else {
                  return val > max ? max : val;
            }
      }

      public static double limit(double val, double min, double max) {
            if (val < min) {
                  return min;
            } else {
                  return val > max ? max : val;
            }
      }

      public static float limitTo1(float val) {
            if (val < 0.0F) {
                  return 0.0F;
            } else {
                  return val > 1.0F ? 1.0F : val;
            }
      }

      public static boolean isAnimatedWater() {
            return gameSettings.ofAnimatedWater != 2;
      }

      public static boolean isGeneratedWater() {
            return gameSettings.ofAnimatedWater == 1;
      }

      public static boolean isAnimatedPortal() {
            return gameSettings.ofAnimatedPortal;
      }

      public static boolean isAnimatedLava() {
            return gameSettings.ofAnimatedLava != 2;
      }

      public static boolean isGeneratedLava() {
            return gameSettings.ofAnimatedLava == 1;
      }

      public static boolean isAnimatedFire() {
            return gameSettings.ofAnimatedFire;
      }

      public static boolean isAnimatedRedstone() {
            return gameSettings.ofAnimatedRedstone;
      }

      public static boolean isAnimatedExplosion() {
            return gameSettings.ofAnimatedExplosion;
      }

      public static boolean isAnimatedFlame() {
            return gameSettings.ofAnimatedFlame;
      }

      public static boolean isAnimatedSmoke() {
            return gameSettings.ofAnimatedSmoke;
      }

      public static boolean isVoidParticles() {
            return gameSettings.ofVoidParticles;
      }

      public static boolean isWaterParticles() {
            return gameSettings.ofWaterParticles;
      }

      public static boolean isRainSplash() {
            return gameSettings.ofRainSplash;
      }

      public static boolean isPortalParticles() {
            return gameSettings.ofPortalParticles;
      }

      public static boolean isPotionParticles() {
            return gameSettings.ofPotionParticles;
      }

      public static boolean isDepthFog() {
            return gameSettings.ofDepthFog;
      }

      public static float getAmbientOcclusionLevel() {
            return isShaders() && Shaders.aoLevel >= 0.0F ? Shaders.aoLevel : gameSettings.ofAoLevel;
      }

      public static String listToString(List list) {
            return listToString(list, ", ");
      }

      public static String listToString(List list, String separator) {
            if (list == null) {
                  return "";
            } else {
                  StringBuffer buf = new StringBuffer(list.size() * 5);

                  for(int i = 0; i < list.size(); ++i) {
                        Object obj = list.get(i);
                        if (i > 0) {
                              buf.append(separator);
                        }

                        buf.append(String.valueOf(obj));
                  }

                  return buf.toString();
            }
      }

      public static String arrayToString(Object[] arr) {
            return arrayToString(arr, ", ");
      }

      public static String arrayToString(Object[] arr, String separator) {
            if (arr == null) {
                  return "";
            } else {
                  StringBuffer buf = new StringBuffer(arr.length * 5);

                  for(int i = 0; i < arr.length; ++i) {
                        Object obj = arr[i];
                        if (i > 0) {
                              buf.append(separator);
                        }

                        buf.append(String.valueOf(obj));
                  }

                  return buf.toString();
            }
      }

      public static String arrayToString(int[] arr) {
            return arrayToString(arr, ", ");
      }

      public static String arrayToString(int[] arr, String separator) {
            if (arr == null) {
                  return "";
            } else {
                  StringBuffer buf = new StringBuffer(arr.length * 5);

                  for(int i = 0; i < arr.length; ++i) {
                        int x = arr[i];
                        if (i > 0) {
                              buf.append(separator);
                        }

                        buf.append(String.valueOf(x));
                  }

                  return buf.toString();
            }
      }

      public static Minecraft getMinecraft() {
            return minecraft;
      }

      public static TextureManager getTextureManager() {
            return minecraft.getTextureManager();
      }

      public static IResourceManager getResourceManager() {
            return minecraft.getResourceManager();
      }

      public static InputStream getResourceStream(ResourceLocation location) throws IOException {
            return getResourceStream(minecraft.getResourceManager(), location);
      }

      public static InputStream getResourceStream(IResourceManager resourceManager, ResourceLocation location) throws IOException {
            IResource res = resourceManager.getResource(location);
            return res == null ? null : res.getInputStream();
      }

      public static IResource getResource(ResourceLocation location) throws IOException {
            return minecraft.getResourceManager().getResource(location);
      }

      public static boolean hasResource(ResourceLocation location) {
            try {
                  IResource res = getResource(location);
                  return res != null;
            } catch (IOException var2) {
                  return false;
            }
      }

      public static boolean hasResource(IResourceManager resourceManager, ResourceLocation location) {
            try {
                  IResource res = resourceManager.getResource(location);
                  return res != null;
            } catch (IOException var3) {
                  return false;
            }
      }

      public static IResourcePack[] getResourcePacks() {
            ResourcePackRepository rep = minecraft.getResourcePackRepository();
            List entries = rep.getRepositoryEntries();
            List list = new ArrayList();
            Iterator it = entries.iterator();

            while(it.hasNext()) {
                  Entry entry = (Entry)it.next();
                  list.add(entry.getResourcePack());
            }

            if (rep.func_148530_e() != null) {
                  list.add(rep.func_148530_e());
            }

            IResourcePack[] rps = (IResourcePack[])((IResourcePack[])list.toArray(new IResourcePack[list.size()]));
            return rps;
      }

      public static String getResourcePackNames() {
            if (minecraft.getResourcePackRepository() == null) {
                  return "";
            } else {
                  IResourcePack[] rps = getResourcePacks();
                  if (rps.length <= 0) {
                        return getDefaultResourcePack().getPackName();
                  } else {
                        String[] names = new String[rps.length];

                        for(int i = 0; i < rps.length; ++i) {
                              names[i] = rps[i].getPackName();
                        }

                        String nameStr = arrayToString((Object[])names);
                        return nameStr;
                  }
            }
      }

      public static IResourcePack getDefaultResourcePack() {
            return minecraft.getResourcePackRepository().rprDefaultResourcePack;
      }

      public static boolean isFromDefaultResourcePack(ResourceLocation loc) {
            IResourcePack rp = getDefiningResourcePack(loc);
            return rp == getDefaultResourcePack();
      }

      public static IResourcePack getDefiningResourcePack(ResourceLocation loc) {
            IResourcePack[] rps = getResourcePacks();

            for(int i = rps.length - 1; i >= 0; --i) {
                  IResourcePack rp = rps[i];
                  if (rp.resourceExists(loc)) {
                        return rp;
                  }
            }

            if (getDefaultResourcePack().resourceExists(loc)) {
                  return getDefaultResourcePack();
            } else {
                  return null;
            }
      }

      public static RenderGlobal getRenderGlobal() {
            return minecraft == null ? null : minecraft.renderGlobal;
      }

      public static int getMaxDynamicTileWidth() {
            return 64;
      }

      public static IIcon getSideGrassTexture(IBlockAccess blockAccess, int x, int y, int z, int side, IIcon icon) {
            if (!isBetterGrass()) {
                  return icon;
            } else {
                  IIcon fullIcon = TextureUtils.iconGrassTop;
                  Block destBlock = Blocks.grass;
                  if (icon == TextureUtils.iconMyceliumSide) {
                        fullIcon = TextureUtils.iconMyceliumTop;
                        destBlock = Blocks.mycelium;
                  }

                  if (isBetterGrassFancy()) {
                        --y;
                        switch(side) {
                        case 2:
                              --z;
                              break;
                        case 3:
                              ++z;
                              break;
                        case 4:
                              --x;
                              break;
                        case 5:
                              ++x;
                        }

                        Block block = blockAccess.getBlock(x, y, z);
                        if (block != destBlock) {
                              return icon;
                        }
                  }

                  return fullIcon;
            }
      }

      public static IIcon getSideSnowGrassTexture(IBlockAccess blockAccess, int x, int y, int z, int side) {
            if (!isBetterGrass()) {
                  return TextureUtils.iconGrassSideSnowed;
            } else {
                  if (isBetterGrassFancy()) {
                        switch(side) {
                        case 2:
                              --z;
                              break;
                        case 3:
                              ++z;
                              break;
                        case 4:
                              --x;
                              break;
                        case 5:
                              ++x;
                        }

                        Block block = blockAccess.getBlock(x, y, z);
                        if (block != Blocks.snow_layer && block != Blocks.snow) {
                              return TextureUtils.iconGrassSideSnowed;
                        }
                  }

                  return TextureUtils.iconSnow;
            }
      }

      public static boolean isBetterGrass() {
            return gameSettings.ofBetterGrass != 3;
      }

      public static boolean isBetterGrassFancy() {
            return gameSettings.ofBetterGrass == 2;
      }

      public static boolean isWeatherEnabled() {
            return gameSettings.ofWeather;
      }

      public static boolean isSkyEnabled() {
            return gameSettings.ofSky;
      }

      public static boolean isSunMoonEnabled() {
            return gameSettings.ofSunMoon;
      }

      public static boolean isSunTexture() {
            if (!isSunMoonEnabled()) {
                  return false;
            } else {
                  return !isShaders() || Shaders.isSun();
            }
      }

      public static boolean isMoonTexture() {
            if (!isSunMoonEnabled()) {
                  return false;
            } else {
                  return !isShaders() || Shaders.isMoon();
            }
      }

      public static boolean isVignetteEnabled() {
            if (isShaders() && !Shaders.isVignette()) {
                  return false;
            } else if (gameSettings.ofVignette == 0) {
                  return gameSettings.fancyGraphics;
            } else {
                  return gameSettings.ofVignette == 2;
            }
      }

      public static boolean isStarsEnabled() {
            return gameSettings.ofStars;
      }

      public static void sleep(long ms) {
            try {
                  Thread.sleep(ms);
            } catch (InterruptedException var3) {
                  var3.printStackTrace();
            }

      }

      public static boolean isTimeDayOnly() {
            return gameSettings.ofTime == 1;
      }

      public static boolean isTimeDefault() {
            return gameSettings.ofTime == 0 || gameSettings.ofTime == 2;
      }

      public static boolean isTimeNightOnly() {
            return gameSettings.ofTime == 3;
      }

      public static boolean isClearWater() {
            return gameSettings.ofClearWater;
      }

      public static int getAnisotropicFilterLevel() {
            return gameSettings.anisotropicFiltering;
      }

      public static boolean isAnisotropicFiltering() {
            return getAnisotropicFilterLevel() > 1;
      }

      public static int getAntialiasingLevel() {
            return antialiasingLevel;
      }

      public static boolean isAntialiasing() {
            return false;
      }

      public static boolean isAntialiasingConfigured() {
            return false;
      }

      public static boolean isMultiTexture() {
            return false;
      }

      public static boolean between(int val, int min, int max) {
            return val >= min && val <= max;
      }

      public static boolean isDrippingWaterLava() {
            return gameSettings.ofDrippingWaterLava;
      }

      public static boolean isBetterSnow() {
            return gameSettings.ofBetterSnow;
      }

      public static Dimension getFullscreenDimension() {
            if (desktopDisplayMode == null) {
                  return null;
            } else if (gameSettings == null) {
                  return new Dimension(desktopDisplayMode.getWidth(), desktopDisplayMode.getHeight());
            } else {
                  String dimStr = gameSettings.ofFullscreenMode;
                  if (dimStr.equals("Default")) {
                        return new Dimension(desktopDisplayMode.getWidth(), desktopDisplayMode.getHeight());
                  } else {
                        String[] dimStrs = tokenize(dimStr, " x");
                        return dimStrs.length < 2 ? new Dimension(desktopDisplayMode.getWidth(), desktopDisplayMode.getHeight()) : new Dimension(parseInt(dimStrs[0], -1), parseInt(dimStrs[1], -1));
                  }
            }
      }

      public static int parseInt(String str, int defVal) {
            try {
                  if (str == null) {
                        return defVal;
                  } else {
                        str = str.trim();
                        return Integer.parseInt(str);
                  }
            } catch (NumberFormatException var3) {
                  return defVal;
            }
      }

      public static float parseFloat(String str, float defVal) {
            try {
                  if (str == null) {
                        return defVal;
                  } else {
                        str = str.trim();
                        return Float.parseFloat(str);
                  }
            } catch (NumberFormatException var3) {
                  return defVal;
            }
      }

      public static boolean parseBoolean(String str, boolean defVal) {
            try {
                  if (str == null) {
                        return defVal;
                  } else {
                        str = str.trim();
                        return Boolean.parseBoolean(str);
                  }
            } catch (NumberFormatException var3) {
                  return defVal;
            }
      }

      public static String[] tokenize(String str, String delim) {
            StringTokenizer tok = new StringTokenizer(str, delim);
            ArrayList list = new ArrayList();

            while(tok.hasMoreTokens()) {
                  String token = tok.nextToken();
                  list.add(token);
            }

            String[] strs = (String[])((String[])list.toArray(new String[list.size()]));
            return strs;
      }

      public static DisplayMode getDesktopDisplayMode() {
            return desktopDisplayMode;
      }

      public static DisplayMode[] getDisplayModes() {
            if (displayModes == null) {
                  try {
                        DisplayMode[] modes = Display.getAvailableDisplayModes();
                        Set setDimensions = getDisplayModeDimensions(modes);
                        List list = new ArrayList();
                        Iterator it = setDimensions.iterator();

                        while(it.hasNext()) {
                              Dimension dim = (Dimension)it.next();
                              DisplayMode[] dimModes = getDisplayModes(modes, dim);
                              DisplayMode dm = getDisplayMode(dimModes, desktopDisplayMode);
                              if (dm != null) {
                                    list.add(dm);
                              }
                        }

                        DisplayMode[] fsModes = (DisplayMode[])((DisplayMode[])list.toArray(new DisplayMode[list.size()]));
                        Arrays.sort(fsModes, new DisplayModeComparator());
                        return fsModes;
                  } catch (Exception var7) {
                        var7.printStackTrace();
                        displayModes = new DisplayMode[]{desktopDisplayMode};
                  }
            }

            return displayModes;
      }

      public static DisplayMode getLargestDisplayMode() {
            DisplayMode[] modes = getDisplayModes();
            if (modes != null && modes.length >= 1) {
                  DisplayMode mode = modes[modes.length - 1];
                  if (desktopDisplayMode.getWidth() > mode.getWidth()) {
                        return desktopDisplayMode;
                  } else {
                        return desktopDisplayMode.getWidth() == mode.getWidth() && desktopDisplayMode.getHeight() > mode.getHeight() ? desktopDisplayMode : mode;
                  }
            } else {
                  return desktopDisplayMode;
            }
      }

      private static Set getDisplayModeDimensions(DisplayMode[] modes) {
            Set set = new HashSet();

            for(int i = 0; i < modes.length; ++i) {
                  DisplayMode mode = modes[i];
                  Dimension dim = new Dimension(mode.getWidth(), mode.getHeight());
                  set.add(dim);
            }

            return set;
      }

      private static DisplayMode[] getDisplayModes(DisplayMode[] modes, Dimension dim) {
            List list = new ArrayList();

            for(int i = 0; i < modes.length; ++i) {
                  DisplayMode mode = modes[i];
                  if ((double)mode.getWidth() == dim.getWidth() && (double)mode.getHeight() == dim.getHeight()) {
                        list.add(mode);
                  }
            }

            DisplayMode[] dimModes = (DisplayMode[])((DisplayMode[])list.toArray(new DisplayMode[list.size()]));
            return dimModes;
      }

      private static DisplayMode getDisplayMode(DisplayMode[] modes, DisplayMode desktopMode) {
            if (desktopMode != null) {
                  for(int i = 0; i < modes.length; ++i) {
                        DisplayMode mode = modes[i];
                        if (mode.getBitsPerPixel() == desktopMode.getBitsPerPixel() && mode.getFrequency() == desktopMode.getFrequency()) {
                              return mode;
                        }
                  }
            }

            if (modes.length <= 0) {
                  return null;
            } else {
                  Arrays.sort(modes, new DisplayModeComparator());
                  return modes[modes.length - 1];
            }
      }

      public static String[] getDisplayModeNames() {
            DisplayMode[] modes = getDisplayModes();
            String[] names = new String[modes.length];

            for(int i = 0; i < modes.length; ++i) {
                  DisplayMode mode = modes[i];
                  String name = "" + mode.getWidth() + "x" + mode.getHeight();
                  names[i] = name;
            }

            return names;
      }

      public static DisplayMode getDisplayMode(Dimension dim) throws LWJGLException {
            DisplayMode[] modes = getDisplayModes();

            for(int i = 0; i < modes.length; ++i) {
                  DisplayMode dm = modes[i];
                  if (dm.getWidth() == dim.width && dm.getHeight() == dim.height) {
                        return dm;
                  }
            }

            return desktopDisplayMode;
      }

      public static boolean isAnimatedTerrain() {
            return gameSettings.ofAnimatedTerrain;
      }

      public static boolean isAnimatedItems() {
            return gameSettings.ofAnimatedItems;
      }

      public static boolean isAnimatedTextures() {
            return gameSettings.ofAnimatedTextures;
      }

      public static boolean isSwampColors() {
            return gameSettings.ofSwampColors;
      }

      public static boolean isRandomMobs() {
            return gameSettings.ofRandomMobs;
      }

      public static void checkGlError(String loc) {
            int i = GL11.glGetError();
            if (i != 0) {
                  String text = GLU.gluErrorString(i);
                  error("OpenGlError: " + i + " (" + text + "), at: " + loc);
            }

      }

      public static boolean isSmoothBiomes() {
            return gameSettings.ofSmoothBiomes;
      }

      public static boolean isCustomColors() {
            return gameSettings.ofCustomColors;
      }

      public static boolean isCustomSky() {
            return gameSettings.ofCustomSky;
      }

      public static boolean isCustomFonts() {
            return gameSettings.ofCustomFonts;
      }

      public static boolean isShowCapes() {
            return gameSettings.ofShowCapes;
      }

      public static boolean isConnectedTextures() {
            return gameSettings.ofConnectedTextures != 3;
      }

      public static boolean isNaturalTextures() {
            return gameSettings.ofNaturalTextures;
      }

      public static boolean isConnectedTexturesFancy() {
            return gameSettings.ofConnectedTextures == 2;
      }

      public static boolean isFastRender() {
            return gameSettings.ofFastRender;
      }

      public static boolean isTranslucentBlocksFancy() {
            return gameSettings.ofTranslucentBlocks == 2;
      }

      public static boolean isShaders() {
            return Shaders.shaderPackLoaded;
      }

      public static String[] readLines(File file) throws IOException {
            FileInputStream fis = new FileInputStream(file);
            return readLines((InputStream)fis);
      }

      public static String[] readLines(InputStream is) throws IOException {
            List list = new ArrayList();
            InputStreamReader isr = new InputStreamReader(is, "ASCII");
            BufferedReader br = new BufferedReader(isr);

            while(true) {
                  String line = br.readLine();
                  if (line == null) {
                        String[] lines = (String[])((String[])list.toArray(new String[list.size()]));
                        return lines;
                  }

                  list.add(line);
            }
      }

      public static String readFile(File file) throws IOException {
            FileInputStream fin = new FileInputStream(file);
            return readInputStream(fin, "ASCII");
      }

      public static String readInputStream(InputStream in) throws IOException {
            return readInputStream(in, "ASCII");
      }

      public static String readInputStream(InputStream in, String encoding) throws IOException {
            InputStreamReader inr = new InputStreamReader(in, encoding);
            BufferedReader br = new BufferedReader(inr);
            StringBuffer sb = new StringBuffer();

            while(true) {
                  String line = br.readLine();
                  if (line == null) {
                        return sb.toString();
                  }

                  sb.append(line);
                  sb.append("\n");
            }
      }

      public static byte[] readAll(InputStream is) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];

            while(true) {
                  int len = is.read(buf);
                  if (len < 0) {
                        is.close();
                        byte[] bytes = baos.toByteArray();
                        return bytes;
                  }

                  baos.write(buf, 0, len);
            }
      }

      public static GameSettings getGameSettings() {
            return gameSettings;
      }

      public static String getNewRelease() {
            return newRelease;
      }

      public static void setNewRelease(String newRelease) {
            Config.newRelease = newRelease;
      }

      public static int compareRelease(String rel1, String rel2) {
            String[] rels1 = splitRelease(rel1);
            String[] rels2 = splitRelease(rel2);
            String branch1 = rels1[0];
            String branch2 = rels2[0];
            if (!branch1.equals(branch2)) {
                  return branch1.compareTo(branch2);
            } else {
                  int rev1 = parseInt(rels1[1], -1);
                  int rev2 = parseInt(rels2[1], -1);
                  if (rev1 != rev2) {
                        return rev1 - rev2;
                  } else {
                        String suf1 = rels1[2];
                        String suf2 = rels2[2];
                        if (!suf1.equals(suf2)) {
                              if (suf1.isEmpty()) {
                                    return 1;
                              }

                              if (suf2.isEmpty()) {
                                    return -1;
                              }
                        }

                        return suf1.compareTo(suf2);
                  }
            }
      }

      private static String[] splitRelease(String relStr) {
            if (relStr != null && relStr.length() > 0) {
                  Pattern p = Pattern.compile("([A-Z])([0-9]+)(.*)");
                  Matcher m = p.matcher(relStr);
                  if (!m.matches()) {
                        return new String[]{"", "", ""};
                  } else {
                        String branch = normalize(m.group(1));
                        String revision = normalize(m.group(2));
                        String suffix = normalize(m.group(3));
                        return new String[]{branch, revision, suffix};
                  }
            } else {
                  return new String[]{"", "", ""};
            }
      }

      public static int intHash(int x) {
            x = x ^ 61 ^ x >> 16;
            x += x << 3;
            x ^= x >> 4;
            x *= 668265261;
            x ^= x >> 15;
            return x;
      }

      public static int getRandom(int x, int y, int z, int face) {
            int rand = intHash(face + 37);
            rand = intHash(rand + x);
            rand = intHash(rand + z);
            rand = intHash(rand + y);
            return rand;
      }

      public static WorldServer getWorldServer() {
            World world = minecraft.theWorld;
            if (world == null) {
                  return null;
            } else if (!minecraft.isIntegratedServerRunning()) {
                  return null;
            } else {
                  IntegratedServer is = minecraft.getIntegratedServer();
                  if (is == null) {
                        return null;
                  } else {
                        WorldProvider wp = world.provider;
                        if (wp == null) {
                              return null;
                        } else {
                              int wd = wp.dimensionId;

                              try {
                                    WorldServer ws = is.worldServerForDimension(wd);
                                    return ws;
                              } catch (NullPointerException var5) {
                                    return null;
                              }
                        }
                  }
            }
      }

      public static int getAvailableProcessors() {
            return availableProcessors;
      }

      public static void updateAvailableProcessors() {
            availableProcessors = Runtime.getRuntime().availableProcessors();
      }

      public static boolean isSingleProcessor() {
            return getAvailableProcessors() <= 1;
      }

      public static boolean isSmoothWorld() {
            return !isSingleProcessor() ? false : gameSettings.ofSmoothWorld;
      }

      public static boolean isLazyChunkLoading() {
            return gameSettings.ofLazyChunkLoading;
      }

      public static int getChunkViewDistance() {
            if (gameSettings == null) {
                  return 10;
            } else {
                  int chunkDistance = gameSettings.renderDistanceChunks;
                  return chunkDistance;
            }
      }

      public static boolean equals(Object o1, Object o2) {
            if (o1 == o2) {
                  return true;
            } else {
                  return o1 == null ? false : o1.equals(o2);
            }
      }

      public static boolean equalsOne(Object a, Object[] bs) {
            if (bs == null) {
                  return false;
            } else {
                  for(int i = 0; i < bs.length; ++i) {
                        Object b = bs[i];
                        if (equals(a, b)) {
                              return true;
                        }
                  }

                  return false;
            }
      }

      public static boolean equalsOne(int val, int[] vals) {
            for(int i = 0; i < vals.length; ++i) {
                  if (vals[i] == val) {
                        return true;
                  }
            }

            return false;
      }

      public static boolean isSameOne(Object a, Object[] bs) {
            if (bs == null) {
                  return false;
            } else {
                  for(int i = 0; i < bs.length; ++i) {
                        Object b = bs[i];
                        if (a == b) {
                              return true;
                        }
                  }

                  return false;
            }
      }

      public static String normalize(String s) {
            return s == null ? "" : s;
      }

    public static void checkDisplaySettings()
    {
        int samples = getAntialiasingLevel();

        if (samples > 0)
        {
            DisplayMode displayMode = Display.getDisplayMode();
            dbg("FSAA Samples: " + samples);

            try
            {
                Display.destroy();
                Display.setDisplayMode(displayMode);
                Display.create((new PixelFormat()).withDepthBits(24).withSamples(samples));
                Display.setResizable(false);
                Display.setResizable(true);
            }
            catch (LWJGLException var9)
            {
                warn("Error setting FSAA: " + samples + "x");
                var9.printStackTrace();

                try
                {
                    Display.setDisplayMode(displayMode);
                    Display.create((new PixelFormat()).withDepthBits(24));
                    Display.setResizable(false);
                    Display.setResizable(true);
                }
                catch (LWJGLException var8)
                {
                    var8.printStackTrace();

                    try
                    {
                        Display.setDisplayMode(displayMode);
                        Display.create();
                        Display.setResizable(false);
                        Display.setResizable(true);
                    }
                    catch (LWJGLException var7)
                    {
                        var7.printStackTrace();
                    }
                }
            }

            if (Util.getOSType() != Util.EnumOS.OSX)
            {
                try
                {
                    File e = new File(minecraft.mcDataDir, "assets");
                    ByteBuffer bufIcon16 = readIconImage(new File(e, "/icons/icon_16x16.png"));
                    ByteBuffer bufIcon32 = readIconImage(new File(e, "/icons/icon_32x32.png"));
                    ByteBuffer[] buf = new ByteBuffer[] {bufIcon16, bufIcon32};
                    Display.setIcon(buf);
                }
                catch (IOException var6)
                {
                    warn(var6.getClass().getName() + ": " + var6.getMessage());
                }
            }
        }
    }

    private static ByteBuffer readIconImage(File par1File) throws IOException
    {
        BufferedImage var2 = ImageIO.read(par1File);
        int[] var3 = var2.getRGB(0, 0, var2.getWidth(), var2.getHeight(), (int[])null, 0, var2.getWidth());
        ByteBuffer var4 = ByteBuffer.allocate(4 * var3.length);
        int[] var5 = var3;
        int var6 = var3.length;

        for (int var7 = 0; var7 < var6; ++var7)
        {
            int var8 = var5[var7];
            var4.putInt(var8 << 8 | var8 >> 24 & 255);
        }

        var4.flip();
        return var4;
    }

      public static void checkDisplayMode() {
            try {
                  if (minecraft.isFullScreen()) {
                        if (fullscreenModeChecked) {
                              return;
                        }

                        fullscreenModeChecked = true;
                        desktopModeChecked = false;
                        DisplayMode mode = Display.getDisplayMode();
                        Dimension dim = getFullscreenDimension();
                        if (dim == null) {
                              return;
                        }

                        if (mode.getWidth() == dim.width && mode.getHeight() == dim.height) {
                              return;
                        }

                        DisplayMode newMode = getDisplayMode(dim);
                        if (newMode == null) {
                              return;
                        }

                        Display.setDisplayMode(newMode);
                        minecraft.displayWidth = Display.getDisplayMode().getWidth();
                        minecraft.displayHeight = Display.getDisplayMode().getHeight();
                        if (minecraft.displayWidth <= 0) {
                              minecraft.displayWidth = 1;
                        }

                        if (minecraft.displayHeight <= 0) {
                              minecraft.displayHeight = 1;
                        }

                        if (minecraft.currentScreen != null) {
                              ScaledResolution sr = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
                              int sw = sr.getScaledWidth();
                              int sh = sr.getScaledHeight();
                              minecraft.currentScreen.setWorldAndResolution(minecraft, sw, sh);
                        }

                        minecraft.loadingScreen = new LoadingScreenRenderer(minecraft);
                        updateFramebufferSize();
                        Display.setFullscreen(true);
                        minecraft.gameSettings.updateVSync();
                        GL11.glEnable(3553);
                  } else {
                        if (desktopModeChecked) {
                              return;
                        }

                        desktopModeChecked = true;
                        fullscreenModeChecked = false;
                        minecraft.gameSettings.updateVSync();
                        Display.update();
                        GL11.glEnable(3553);
                        Display.setResizable(false);
                        Display.setResizable(true);
                  }
            } catch (Exception var6) {
                  var6.printStackTrace();
                  gameSettings.ofFullscreenMode = "Default";
                  gameSettings.saveOfOptions();
            }

      }

      public static void updateFramebufferSize() {
            minecraft.getFramebuffer().createBindFramebuffer(minecraft.displayWidth, minecraft.displayHeight);
            if (minecraft.entityRenderer != null) {
                  minecraft.entityRenderer.updateShaderGroupSize(minecraft.displayWidth, minecraft.displayHeight);
            }

      }

      public static Object[] addObjectToArray(Object[] arr, Object obj) {
            if (arr == null) {
                  throw new NullPointerException("The given array is NULL");
            } else {
                  int arrLen = arr.length;
                  int newLen = arrLen + 1;
                  Object[] newArr = (Object[])((Object[])Array.newInstance(arr.getClass().getComponentType(), newLen));
                  System.arraycopy(arr, 0, newArr, 0, arrLen);
                  newArr[arrLen] = obj;
                  return newArr;
            }
      }

      public static Object[] addObjectToArray(Object[] arr, Object obj, int index) {
            List list = new ArrayList(Arrays.asList(arr));
            list.add(index, obj);
            Object[] newArr = (Object[])((Object[])Array.newInstance(arr.getClass().getComponentType(), list.size()));
            return list.toArray(newArr);
      }

      public static Object[] addObjectsToArray(Object[] arr, Object[] objs) {
            if (arr == null) {
                  throw new NullPointerException("The given array is NULL");
            } else if (objs.length == 0) {
                  return arr;
            } else {
                  int arrLen = arr.length;
                  int newLen = arrLen + objs.length;
                  Object[] newArr = (Object[])((Object[])Array.newInstance(arr.getClass().getComponentType(), newLen));
                  System.arraycopy(arr, 0, newArr, 0, arrLen);
                  System.arraycopy(objs, 0, newArr, arrLen, objs.length);
                  return newArr;
            }
      }

      public static boolean isCustomItems() {
            return false;
      }

      public static void drawFps() {
            String debugStr = minecraft.debug;
            int pos = debugStr.indexOf(32);
            if (pos < 0) {
                  pos = 0;
            }

            String fps = debugStr.substring(0, pos);
            String updates = getUpdates(minecraft.debug);
            int renderersActive = minecraft.renderGlobal.getCountActiveRenderers();
            int entities = minecraft.renderGlobal.getCountEntitiesRendered();
            int tileEntities = minecraft.renderGlobal.getCountTileEntitiesRendered();
            String fpsStr = fps + " fps, C: " + renderersActive + ", E: " + entities + "+" + tileEntities + ", U: " + updates;
            minecraft.fontRenderer.drawString(fpsStr, 2, 2, -2039584);
      }

      private static String getUpdates(String str) {
            int pos1 = str.indexOf(", ");
            if (pos1 < 0) {
                  return "";
            } else {
                  pos1 += 2;
                  int pos2 = str.indexOf(32, pos1);
                  return pos2 < 0 ? "" : str.substring(pos1, pos2);
            }
      }

      public static int getBitsOs() {
            String progFiles86 = System.getenv("ProgramFiles(X86)");
            return progFiles86 != null ? 64 : 32;
      }

      public static int getBitsJre() {
            String[] propNames = new String[]{"sun.arch.data.model", "com.ibm.vm.bitmode", "os.arch"};

            for(int i = 0; i < propNames.length; ++i) {
                  String propName = propNames[i];
                  String propVal = System.getProperty(propName);
                  if (propVal != null && propVal.contains("64")) {
                        return 64;
                  }
            }

            return 32;
      }

      public static boolean isNotify64BitJava() {
            return notify64BitJava;
      }

      public static void setNotify64BitJava(boolean flag) {
            notify64BitJava = flag;
      }

      public static boolean isConnectedModels() {
            return false;
      }

      public static void showGuiMessage(String line1, String line2) {
            GuiMessage gui = new GuiMessage(minecraft.currentScreen, line1, line2);
            minecraft.displayGuiScreen(gui);
      }

      public static int[] addIntToArray(int[] intArray, int intValue) {
            return addIntsToArray(intArray, new int[]{intValue});
      }

      public static int[] addIntsToArray(int[] intArray, int[] copyFrom) {
            if (intArray != null && copyFrom != null) {
                  int arrLen = intArray.length;
                  int newLen = arrLen + copyFrom.length;
                  int[] newArray = new int[newLen];
                  System.arraycopy(intArray, 0, newArray, 0, arrLen);

                  for(int index = 0; index < copyFrom.length; ++index) {
                        newArray[index + arrLen] = copyFrom[index];
                  }

                  return newArray;
            } else {
                  throw new NullPointerException("The given array is NULL");
            }
      }

      public static DynamicTexture getMojangLogoTexture(DynamicTexture texDefault) {
            try {
                  ResourceLocation locationMojangPng = new ResourceLocation("textures/gui/title/mojang.png");
                  InputStream in = getResourceStream(locationMojangPng);
                  if (in == null) {
                        return texDefault;
                  } else {
                        BufferedImage bi = ImageIO.read(in);
                        if (bi == null) {
                              return texDefault;
                        } else {
                              DynamicTexture dt = new DynamicTexture(bi);
                              return dt;
                        }
                  }
            } catch (Exception var5) {
                  warn(var5.getClass().getName() + ": " + var5.getMessage());
                  return texDefault;
            }
      }

      public static void writeFile(File file, String str) throws IOException {
            FileOutputStream fos = new FileOutputStream(file);
            byte[] bytes = str.getBytes("ASCII");
            fos.write(bytes);
            fos.close();
      }

      public static boolean isDynamicFov() {
            return gameSettings.ofDynamicFov;
      }

      public static TextureMap getTextureMap() {
            return getMinecraft().getTextureMapBlocks();
      }

      public static boolean isDynamicLights() {
            return gameSettings.ofDynamicLights != 3;
      }

      public static boolean isDynamicLightsFast() {
            return gameSettings.ofDynamicLights == 1;
      }

      public static boolean isDynamicHandLight() {
            if (!isDynamicLights()) {
                  return false;
            } else {
                  return isShaders() ? Shaders.isDynamicHandLight() : true;
            }
      }

      public static int[] toPrimitive(Integer[] arr) {
            if (arr == null) {
                  return null;
            } else if (arr.length == 0) {
                  return new int[0];
            } else {
                  int[] intArr = new int[arr.length];

                  for(int i = 0; i < intArr.length; ++i) {
                        intArr[i] = arr[i];
                  }

                  return intArr;
            }
      }
}

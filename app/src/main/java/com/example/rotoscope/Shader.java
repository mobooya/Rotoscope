package com.example.rotoscope;

public class Shader {
	
    public static String mVertexShader =
		"uniform mat4 uMVPMatrix;\n" +
		"uniform mat4 uSTMatrix;\n" +
		"attribute vec4 aPosition;\n" +
		"attribute vec4 aTextureCoord;\n" +
		"varying vec2 vTextureCoord;\n" +
		"void main() {\n" +
		"  gl_Position = uMVPMatrix * aPosition;\n" +
		"  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
		"}\n";

    public static String mOriginalFragmentShader =
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"void main() {\n" +
		"  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
		"}\n";


    public static final String laplacianFragShaderCode = 
    		"#extension GL_OES_EGL_image_external : require\n"+
    		"precision mediump float;\n" +    		
    		"const int MaxKernelSize = 25;\n" +

			//"// size of kernel (width * height) for this execution" +
			"int KernelSize = 25;\n" +
			
			//"// image to be convolved" +
			"uniform samplerExternalOES sceneTex;\n" +
			"varying vec2 vTextureCoord;\n" + 
			"void main() {\n" +
			"    int i;\n" +
			
			//"// value for each location in the convolution kernel" +
			"float KernelValue[MaxKernelSize];\n" + 
				"KernelValue[0] = -1.0;\n" +
				"KernelValue[1] = -1.0;\n" +
				"KernelValue[2] = -1.0;\n" +
				"KernelValue[3] = -1.0;\n" +
				"KernelValue[4] = -1.0;\n" +
				"KernelValue[5] = -1.0;\n" +
				"KernelValue[6] = -1.0;\n" +
				"KernelValue[7] = -1.0;\n" +
				"KernelValue[8] = -1.0;\n" +
				"KernelValue[9] = -1.0;\n" +
				"KernelValue[10] = -1.0;\n" +
				"KernelValue[11] = -1.0;\n" +
				"KernelValue[12] = 24.0;\n" +
				"KernelValue[13] = -1.0;\n" +
				"KernelValue[14] = -1.0;\n" +
				"KernelValue[15] = -1.0;\n" +
				"KernelValue[16] = -1.0;\n" +
				"KernelValue[17] = -1.0;\n" +
				"KernelValue[18] = -1.0;\n" +
				"KernelValue[19] = -1.0;\n" +
				"KernelValue[20] = -1.0;\n" +
				"KernelValue[21] = -1.0;\n" +
				"KernelValue[22] = -1.0;\n" +
				"KernelValue[23] = -1.0;\n" +
				"KernelValue[24] = -1.0;\n" +

			//"// array of offsets for accessing the base image" +
			"vec2 Offsets[MaxKernelSize];\n" +
			"Offsets[0] = vec2( -2.0 / 240.0, -2.0 / 320.0);\n " + 
			   "Offsets[1] = vec2( -2.0 / 240.0, -1.0 / 320.0);\n " +
			   "Offsets[2] = vec2( -2.0 / 240.0, 0.0);\n " +
			   "Offsets[3] = vec2( -2.0 / 240.0, 1.0 / 320.0);\n " +
			   "Offsets[4] = vec2( -2.0 / 240.0, 2.0 / 320.0);\n " +
										
			   "Offsets[5] = vec2( -1.0 / 240.0, -2.0 / 320.0);\n " +
			   "Offsets[6] = vec2( -1.0 / 240.0, -1.0 / 320.0);\n " +
			   "Offsets[7] = vec2( -1.0 / 240.0, 0.0);\n " +
			   "Offsets[8] = vec2( -1.0 / 240.0, 1.0 / 320.0);\n " +
			   "Offsets[9] = vec2( -1.0 / 240.0, 2.0 / 320.0);\n " +
										
			    "Offsets[10] = vec2(0.0, -2.0 / 320.0);\n" + 
			    "Offsets[11] = vec2(0.0, -1.0 / 320.0);\n" + 
			    "Offsets[12] = vec2(0.0, 0.0);\n" +
										
			    "Offsets[13] = vec2(0.0, 1.0 / 320.0);\n" + 
			    "Offsets[14] = vec2(0.0, 2.0 / 320.0);\n " +
										
			    "Offsets[15] = vec2(1.0 / 240.0, -2.0 / 320.0);\n" + 
			    "Offsets[16] = vec2(1.0 / 240.0, -1.0 / 320.0);\n " +
			    "Offsets[17] = vec2(1.0 / 240.0, 0.0);\n " +
			    "Offsets[18] = vec2(1.0 / 240.0, 1.0 / 320.0);\n " +
			    "Offsets[19] = vec2(1.0 / 240.0, 2.0 / 320.0);\n " +
										
			    "Offsets[20] = vec2(2.0 / 240.0, -2.0 / 320.0);\n " +
			    "Offsets[21] = vec2(2.0 / 240.0, -1.0 / 320.0);\n " +
			    "Offsets[22] = vec2(2.0 / 240.0, 0.0);\n " +
			    "Offsets[23] = vec2(2.0 / 240.0, 1.0 / 320.0);\n " +
			    "Offsets[24] = vec2(2.0 / 240.0, 2.0 / 320.0);\n" +
										
			"    vec4 color = texture2D(sceneTex, vTextureCoord.st).rgba;\n" +
			"    vec4 sum = vec4(0.0);\n" +
			"	vec4 coloroutput = vec4(0,0,0,1);\n" +
				
			"    for (i = 0; i < KernelSize; i++)\n" +
			"    {\n" +
			"        vec4 tmp = texture2D(sceneTex, vTextureCoord.st + Offsets[i]);\n" +
			"        vec4 v = vec4(KernelValue[i], KernelValue[i], KernelValue[i], KernelValue[i]);\n" +
			"        sum += tmp * v;\n" +
			"    }\n" +
		    
			"    if (any(greaterThan(vec3(2.0, 2.0, 2.0), vec3(sum.x, sum.y, sum.z))))\n" +
			"    	coloroutput = color;\n" +
			"    else\n" +
			"		coloroutput = vec4(0.0, 0.0, 0.0, 1.0); \n" +
			"    gl_FragColor = coloroutput;\n" +			
			"}";


    public static String mGrayScaleFragShader = 
		"#extension GL_OES_EGL_image_external : require\n" +
			"precision mediump float;\n" +
			"varying vec2 vTextureCoord;\n" +
			"uniform samplerExternalOES sTexture;\n" +
			"void main() {\n" +
			"vec4 color = texture2D(sTexture, vTextureCoord.st).rgba;\n" +
			
			"float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));\n" +
		    
		    "gl_FragColor = vec4(luma, luma, luma, 1.0);}\n";
		 
    public static String mPixellateFragShader = 
    		"#extension GL_OES_EGL_image_external : require\n" +
    			"precision mediump float;\n" +
    			"varying vec2 vTextureCoord;\n" +
    			"uniform samplerExternalOES sTexture;\n" +
    			"void main() {\n" +
    			"float dx = 15.*(1./2048.);\n" +
    			"float dy = 10.*(1./2048.);\n" +
    			"vec2 coord = vec2(dx*floor(vTextureCoord.x/dx),\n" +
                "dy*floor(vTextureCoord.y/dy));\n" +
                "gl_FragColor = texture2D(sTexture, coord);}\n";
				
	public static final String mTileFragShader = 
			"#extension GL_OES_EGL_image_external : require\n" +
    			"precision mediump float;\n" +
				"const float NumTiles = 100.0;\n" +
				"const float Threshhold = 0.9999;\n" +
				"const vec3 EdgeColor = vec3(0.0, 0.0, 0.0);\n" +
    			"varying vec2 vTextureCoord;\n" +
    			"uniform samplerExternalOES sTexture;\n" +
    			"void main() {\n" +
				"float size = 1.0/NumTiles;\n" +
				"vec2 Pbase = vTextureCoord.st - mod(vTextureCoord.st,vec2(size, size));\n" +
				"vec2 PCenter = Pbase + vec2(size/2.0, size/2.0);\n" +
				"vec2 st = (vTextureCoord.st - Pbase)/size;\n" +
				"vec4 c1 = vec4(0.0, 0.0, 0.0, 0.0);\n" +
				"vec4 c2 = vec4(0.0, 0.0, 0.0, 0.0);\n" +
				"vec4 invOff = vec4((1.0-EdgeColor),1.0);\n" +
				"if (st.x > st.y) { c1 = invOff; }\n" +
				"float threshholdB =  1.0 - Threshhold;\n" +
				"if (st.x > threshholdB) { c2 = c1; }\n" +
				"if (st.y > threshholdB) { c2 = c1; }\n" +
				"vec4 cBottom = c2;\n" +
				"c1 = vec4(0.0, 0.0, 0.0, 0.0);\n" +
				"c2 = vec4(0.0, 0.0, 0.0, 0.0);\n" +
				"if (st.x > st.y) { c1 = invOff; }\n" +
				"if (st.x < Threshhold) { c2 = c1; }\n" +
				"if (st.y < Threshhold) { c2 = c1; }\n" +
				"vec4 cTop = c2;\n" +
				"vec4 tileColor = texture2D(sTexture,PCenter);\n" +
				"vec4 result = tileColor + cTop - cBottom;\n" +
				"gl_FragColor = result;}\n";
				
    public static final String mPosterizeFragShader =
		"#extension GL_OES_EGL_image_external : require\n" +
    	"precision mediump float;\n" +
		"const float Gamma = 1.0;\n" +
		"const float NColors = 8.0;\n" +
		"varying vec2 vTextureCoord;\n" +
    	"uniform samplerExternalOES sTexture;\n" +
    	"void main() {\n" +
		"vec4 texCol = texture2D(sTexture, vTextureCoord.st);\n" +
		"vec3 tc = texCol.xyz;\n" +
		"tc = pow(tc, vec3(Gamma, Gamma, Gamma));\n" +
		"tc = tc * NColors;\n" +
		"tc = floor(tc);\n" +
		"tc = tc / NColors;\n" +
		"tc = pow(tc,vec3(1.0/Gamma, 1.0/Gamma, 1.0/Gamma));\n" +
		"gl_FragColor = vec4(tc,texCol.w);}\n";
    
	public static final String mKuwaharaFragShader = 
			"#extension GL_OES_EGL_image_external : require\n" +
			"precision lowp float;\n" +
			"varying vec2 vTextureCoord;\n" +
			"uniform samplerExternalOES sTexture;\n" +
			"const float NPixels = 1.5;\n" +
			"const vec2 QuadScreenSize = vec2(480.0,800.0);\n" +
			"float getGray(vec4 c)\n" +
			"{\n" +
				"return(dot(c.rgb,(vec3(0.33333))));\n" +
			"}\n" +

			"#define GrabPix(n,a) vec3 n = texture2D(sTexture,(a)).xyz;\n" +

			"void main() {\n" +
			"vec2 ox = vec2(NPixels/QuadScreenSize.x,0.0);\n" +
			"vec2 oy = vec2(0.0,NPixels/QuadScreenSize.y);\n" +
			"vec2 uv = vTextureCoord.xy;\n" +
			"vec2 ox2 = 2. * ox;\n" +
			"vec2 oy2 = 2. * oy;\n" +
			"vec2 PP = uv - oy2;\n" +
			"GrabPix(c00,PP-ox2)\n" +
			"GrabPix(c01,PP-ox)\n" +
			"GrabPix(c02,PP)\n" +
			"GrabPix(c03,PP+ox)\n" +
			"GrabPix(c04,PP+ox2)\n" +
			"PP = uv - oy;\n" +
			"GrabPix(c10,PP-ox2)\n" +
			"GrabPix(c11,PP-ox)\n" +
			"GrabPix(c12,PP)\n" +
			"GrabPix(c13,PP+ox)\n" +
			"GrabPix(c14,PP+ox2)\n" +
			"PP = uv;\n" +
			"GrabPix(c20,PP-ox2)\n" +
			"GrabPix(c21,PP-ox)\n" +
			"GrabPix(c22,PP)\n" +
			"GrabPix(c23,PP+ox)\n" +
			"GrabPix(c24,PP+ox2)\n" +
			"vec3 m00 = (c00+c01+c02 + c10+c11+c12 + c20+c21+c22)/9.;\n" +
			"vec3 d = (c00 - m00); float v00 = dot(d,d);\n" +
			"d = (c01 - m00); v00 += dot(d,d);\n" +
			"d = (c02 - m00); v00 += dot(d,d);\n" +
			"d = (c10 - m00); v00 += dot(d,d);\n" +
			"d = (c11 - m00); v00 += dot(d,d);\n" +
			"d = (c12 - m00); v00 += dot(d,d);\n" +
			"d = (c20 - m00); v00 += dot(d,d);\n" +
			"d = (c21 - m00); v00 += dot(d,d);\n" +
			"d = (c12 - m00); v00 += dot(d,d);\n" +
			"vec3 m01 = (c02+c03+c04 + c12+c13+c14 + c22+c23+c24)/9.;\n" +
			"d = (c02 - m01); float v01 = dot(d,d);\n" +
			"d = (c03 - m01); v01 += dot(d,d);\n" +
			"d = (c04 - m01); v01 += dot(d,d);\n" +
			"d = (c12 - m01); v01 += dot(d,d);\n" +
			"d = (c13 - m01); v01 += dot(d,d);\n" +
			"d = (c14 - m01); v01 += dot(d,d);\n" +
			"d = (c22 - m01); v01 += dot(d,d);\n" +
			"d = (c23 - m01); v01 += dot(d,d);\n" +
			"d = (c14 - m01); v01 += dot(d,d);\n" +
			"PP = uv + oy;\n" +
			"GrabPix(c30,PP-ox2)\n" +
			"GrabPix(c31,PP-ox)\n" +
			"GrabPix(c32,PP)\n" +
			"GrabPix(c33,PP+ox)\n" +
			"GrabPix(c34,PP+ox2)\n" +
			"PP = uv + oy;\n" +
			"GrabPix(c40,PP-ox2)\n" +
			"GrabPix(c41,PP-ox)\n" +
			"GrabPix(c42,PP)\n" +
			"GrabPix(c43,PP+ox)\n" +
			"GrabPix(c44,PP+ox2)\n" +
			"vec3 m10 = (c20+c21+c22 + c30+c31+c32 + c40+c41+c42)/9.;\n" +
			"d = (c20 - m10); float v10 = dot(d,d);\n" +
			"d = (c21 - m10); v10 += dot(d,d);\n" +
			"d = (c22 - m10); v10 += dot(d,d);\n" +
			"d = (c30 - m10); v10 += dot(d,d);\n" +
			"d = (c31 - m10); v10 += dot(d,d);\n" +
			"d = (c32 - m10); v10 += dot(d,d);\n" +
			"d = (c40 - m10); v10 += dot(d,d);\n" +
			"d = (c41 - m10); v10 += dot(d,d);\n" +
			"d = (c42 - m10); v10 += dot(d,d);\n" +
			"vec3 m11 = (c22+c23+c24 + c32+c33+c34 + c42+c43+c44)/9.;\n" +
			"d = (c22 - m11); float v11 = dot(d,d);\n" +
			"d = (c23 - m11); v11 += dot(d,d);\n" +
			"d = (c24 - m11); v11 += dot(d,d);\n" +
			"d = (c32 - m11); v11 += dot(d,d);\n" +
			"d = (c33 - m11); v11 += dot(d,d);\n" +
			"d = (c34 - m11); v11 += dot(d,d);\n" +
			"d = (c42 - m11); v11 += dot(d,d);\n" +
			"d = (c43 - m11); v11 += dot(d,d);\n" +
			"d = (c44 - m11); v11 += dot(d,d);\n" +
			"vec3 result = m00;\n" +
			"float rv = v00;\n" +
			"if (v01 < rv) { result = m01; rv = v01; }\n" +
			"if (v10 < rv) { result = m10; rv = v10; }\n" +
			"if (v11 < rv) { result = m11; }\n" +
			"gl_FragColor = vec4(result,1.);}\n";
    
    public static final String neonFragShaderCode = 
    		"#extension GL_OES_EGL_image_external : require\n"+
    		"precision mediump float;\n" +    		
    		"const int MaxKernelSize = 25;\n" +

			//"// size of kernel (width * height) for this execution" +
			"int KernelSize = 25;\n" +
			
			//"// image to be convolved" +
			"uniform samplerExternalOES sceneTex;\n" +
			"varying vec2 vTextureCoord;\n" + 
			"void main() {\n" +
			"    int i;\n" +
			
			//"// value for each location in the convolution kernel" +
			"float KernelValue[MaxKernelSize];\n" + 
				"KernelValue[0] = -1.0;\n" +
				"KernelValue[1] = -1.0;\n" +
				"KernelValue[2] = -1.0;\n" +
				"KernelValue[3] = -1.0;\n" +
				"KernelValue[4] = -1.0;\n" +
				"KernelValue[5] = -1.0;\n" +
				"KernelValue[6] = -1.0;\n" +
				"KernelValue[7] = -1.0;\n" +
				"KernelValue[8] = -1.0;\n" +
				"KernelValue[9] = -1.0;\n" +
				"KernelValue[10] = -1.0;\n" +
				"KernelValue[11] = -1.0;\n" +
				"KernelValue[12] = 24.0;\n" +
				"KernelValue[13] = -1.0;\n" +
				"KernelValue[14] = -1.0;\n" +
				"KernelValue[15] = -1.0;\n" +
				"KernelValue[16] = -1.0;\n" +
				"KernelValue[17] = -1.0;\n" +
				"KernelValue[18] = -1.0;\n" +
				"KernelValue[19] = -1.0;\n" +
				"KernelValue[20] = -1.0;\n" +
				"KernelValue[21] = -1.0;\n" +
				"KernelValue[22] = -1.0;\n" +
				"KernelValue[23] = -1.0;\n" +
				"KernelValue[24] = -1.0;\n" +

			//"// array of offsets for accessing the base image" +
			"vec2 Offsets[MaxKernelSize];\n" +
			"Offsets[0] = vec2( -2.0 / 240.0, -2.0 / 320.0);\n " + 
			   "Offsets[1] = vec2( -2.0 / 240.0, -1.0 / 320.0);\n " +
			   "Offsets[2] = vec2( -2.0 / 240.0, 0.0);\n " +
			   "Offsets[3] = vec2( -2.0 / 240.0, 1.0 / 320.0);\n " +
			   "Offsets[4] = vec2( -2.0 / 240.0, 2.0 / 320.0);\n " +
										
			   "Offsets[5] = vec2( -1.0 / 240.0, -2.0 / 320.0);\n " +
			   "Offsets[6] = vec2( -1.0 / 240.0, -1.0 / 320.0);\n " +
			   "Offsets[7] = vec2( -1.0 / 240.0, 0.0);\n " +
			   "Offsets[8] = vec2( -1.0 / 240.0, 1.0 / 320.0);\n " +
			   "Offsets[9] = vec2( -1.0 / 240.0, 2.0 / 320.0);\n " +
										
			    "Offsets[10] = vec2(0.0, -2.0 / 320.0);\n" + 
			    "Offsets[11] = vec2(0.0, -1.0 / 320.0);\n" + 
			    "Offsets[12] = vec2(0.0, 0.0);\n" +
										
			    "Offsets[13] = vec2(0.0, 1.0 / 320.0);\n" + 
			    "Offsets[14] = vec2(0.0, 2.0 / 320.0);\n " +
										
			    "Offsets[15] = vec2(1.0 / 240.0, -2.0 / 320.0);\n" + 
			    "Offsets[16] = vec2(1.0 / 240.0, -1.0 / 320.0);\n " +
			    "Offsets[17] = vec2(1.0 / 240.0, 0.0);\n " +
			    "Offsets[18] = vec2(1.0 / 240.0, 1.0 / 320.0);\n " +
			    "Offsets[19] = vec2(1.0 / 240.0, 2.0 / 320.0);\n " +
										
			    "Offsets[20] = vec2(2.0 / 240.0, -2.0 / 320.0);\n " +
			    "Offsets[21] = vec2(2.0 / 240.0, -1.0 / 320.0);\n " +
			    "Offsets[22] = vec2(2.0 / 240.0, 0.0);\n " +
			    "Offsets[23] = vec2(2.0 / 240.0, 1.0 / 320.0);\n " +
			    "Offsets[24] = vec2(2.0 / 240.0, 2.0 / 320.0);\n" +
										
			"    vec4 color = texture2D(sceneTex, vTextureCoord.st).rgba;\n" +
			"    vec4 sum = vec4(0.0);\n" +
			"	vec4 coloroutput = vec4(0,0,0,1);\n" +
				
			"    for (i = 0; i < KernelSize; i++)\n" +
			"    {\n" +
			"        vec4 tmp = texture2D(sceneTex, vTextureCoord.st + Offsets[i]);\n" +
			"        vec4 v = vec4(KernelValue[i], KernelValue[i], KernelValue[i], KernelValue[i]);\n" +
			"        sum += tmp * v;\n" +
			"    }\n" +
		    
			"    if (all(greaterThan(vec3(.80, .80, .80), vec3(sum.x, sum.y, sum.z))))\n" +
			"    	coloroutput = color;\n" +
			"    else\n" +
			"		coloroutput = vec4(sum.xyz, 1.0); \n" +
			"    gl_FragColor = coloroutput;\n" +			
			"}";

    public static final String freichenFragShaderCode = 
    		//"#version 300 es\n"+
    		"#extension GL_OES_EGL_image_external : require\n"+
    		"precision mediump float;\n" +    		
			"uniform samplerExternalOES image;\n" +
			"varying vec2 vTextureCoord;\n" +
			
    		"void main(void){\n" +
    		"	mat3 I;\n" +
    		"	float cnv[9];\n" +
    		"	vec3 sample;\n" +
    		"	mat3 G[9];\n" +
    		"   G[0] = 1.0/(2.0*sqrt(2.0)) * mat3( 1.0, sqrt(2.0), 1.0, 0.0, 0.0, 0.0, -1.0, -sqrt(2.0), -1.0 ); \n" +
    		"	G[1] = 1.0/(2.0*sqrt(2.0)) * mat3( 1.0, 0.0, -1.0, sqrt(2.0), 0.0, -sqrt(2.0), 1.0, 0.0, -1.0 ); \n" +
    		"	G[2] = 1.0/(2.0*sqrt(2.0)) * mat3( 0.0, -1.0, sqrt(2.0), 1.0, 0.0, -1.0, -sqrt(2.0), 1.0, 0.0 ); \n" +
    		"	G[3] = 1.0/(2.0*sqrt(2.0)) * mat3( sqrt(2.0), -1.0, 0.0, -1.0, 0.0, 1.0, 0.0, 1.0, -sqrt(2.0) ); \n" +
    		"	G[4] = 1.0/2.0 * mat3( 0.0, 1.0, 0.0, -1.0, 0.0, -1.0, 0.0, 1.0, 0.0 ); \n" +
    		"	G[5] = 1.0/2.0 * mat3( -1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0 ); \n" +
    		"	G[6] = 1.0/6.0 * mat3( 1.0, -2.0, 1.0, -2.0, 4.0, -2.0, 1.0, -2.0, 1.0 ); \n" +
    		"	G[7] = 1.0/6.0 * mat3( -2.0, 1.0, -2.0, 1.0, 4.0, 1.0, -2.0, 1.0, -2.0 ); \n" +
    		"	G[8] = 1.0/3.0 * mat3( 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0 ); \n" +
    		" \n" +
    	
    		"	/* fetch the 3x3 neighbourhood and use the RGB vector's length as intensity value */  \n" +
    		"	for (int i=0; i<3; i++) {\n" +
    		"		for (int j=0; j<3; j++) { \n" +
    	//	"			vec2 mycoord = vec2(240.0,320.0) * vTextureCoord.st; \n" +
    		"			vec2 myoffsets = vec2(i-1,j-1) / vec2(240.0,320.0); \n" +
    		"			sample = texture2D( image, vTextureCoord + myoffsets).rgb; \n" +
		//	"			sample = texture2DOffset( image, vTextureCoord, ivec2(i-1,j-1)).rgb; \n" +
    		"			I[i][j] = length(sample); " +
    		"		} \n" +
    		"	} \n" +
    	
    		"	/* calculate the convolution values for all the masks */ \n" +
    		" 	for (int i=0; i<9; i++) { \n" +
    		"		float dp3 = dot(G[i][0], I[0]) + dot(G[i][1], I[1]) + dot(G[i][2], I[2]); \n" +
    		"		cnv[i] = dp3 * dp3;  \n" +
    		"	} \n" +

    		"	float M = (cnv[0] + cnv[1]) + (cnv[2] + cnv[3]); \n" +
    		"	float S = (cnv[4] + cnv[5]) + (cnv[6] + cnv[7]) + (cnv[8] + M);  \n" +
    	
//			" 	vec4 coloroutput = vec4(0,0,0,1);\n" +
			"	vec4 coloroutput = texture2D( image, vTextureCoord);\n" +
			"	vec4 edge = vec4(sqrt(M/S));\n" + //vec4(1.0) -

			"   if (all(lessThan(vec3(.20, .20, .20), vec3(edge.x, edge.y, edge.z)))){\n" +
//			"    	coloroutput = texture2D( image, vTextureCoord);\n" +
			"    	coloroutput = vec4( (coloroutput.x +((1.0-coloroutput.x)/2.0)), \n" +
			"                           (coloroutput.y +((1.0-coloroutput.y)/2.0)), \n" +
			"                           (coloroutput.z +((1.0-coloroutput.z)/2.0)), \n" +
			"                           1.0 );\n" +
			"    	coloroutput = coloroutput * edge;\n" +
            "   }\n" +
			"   else\n{\n" +
//			"		coloroutput = vec4(0.0,0.0,0.0,1.0); \n" +
			"		coloroutput = edge; \n" +
            "   }\n" +
			"	gl_FragColor = coloroutput; \n" +
    		"}";
    
    public static final String cartoonifyFragShaderCode = 
    		//"#version 300\n"+
    		"#extension GL_OES_EGL_image_external : require\n"+
    		"precision mediump float;\n" +    		
			"uniform samplerExternalOES image;\n" +
			"varying vec2 vTextureCoord;\n" +
			
    		"void main(void){\n" +
    		"	mat3 I;\n" +
    		"	float cnv[9];\n" +
    		"	vec3 sample;\n" +
    		"	mat3 G[9];\n" +
    		"   G[0] = 1.0/(2.0*sqrt(2.0)) * mat3( 1.0, sqrt(2.0), 1.0, 0.0, 0.0, 0.0, -1.0, -sqrt(2.0), -1.0 ); \n" +
    		"	G[1] = 1.0/(2.0*sqrt(2.0)) * mat3( 1.0, 0.0, -1.0, sqrt(2.0), 0.0, -sqrt(2.0), 1.0, 0.0, -1.0 ); \n" +
    		"	G[2] = 1.0/(2.0*sqrt(2.0)) * mat3( 0.0, -1.0, sqrt(2.0), 1.0, 0.0, -1.0, -sqrt(2.0), 1.0, 0.0 ); \n" +
    		"	G[3] = 1.0/(2.0*sqrt(2.0)) * mat3( sqrt(2.0), -1.0, 0.0, -1.0, 0.0, 1.0, 0.0, 1.0, -sqrt(2.0) ); \n" +
    		"	G[4] = 1.0/2.0 * mat3( 0.0, 1.0, 0.0, -1.0, 0.0, -1.0, 0.0, 1.0, 0.0 ); \n" +
    		"	G[5] = 1.0/2.0 * mat3( -1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0 ); \n" +
    		"	G[6] = 1.0/6.0 * mat3( 1.0, -2.0, 1.0, -2.0, 4.0, -2.0, 1.0, -2.0, 1.0 ); \n" +
    		"	G[7] = 1.0/6.0 * mat3( -2.0, 1.0, -2.0, 1.0, 4.0, 1.0, -2.0, 1.0, -2.0 ); \n" +
    		"	G[8] = 1.0/3.0 * mat3( 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0 ); \n" +
    		" \n" +
    	
    		"	/* fetch the 3x3 neighbourhood and use the RGB vector's length as intensity value */  \n" +
    		"	for (int i=0; i<3; i++) {\n" +
    		"		for (int j=0; j<3; j++) { \n" +
    	//	"			vec2 mycoord = vec2(240.0,320.0) * vTextureCoord.st; \n" +
    		"			vec2 myoffsets = vec2(i-1,j-1) / vec2(240.0,320.0); \n" +
    		"			sample = texture2D( image, vTextureCoord + myoffsets).rgb; \n" +
		//	"			sample = texture2DOffset( image, vTextureCoord, ivec2(i-1,j-1)).rgb; \n" +
    		"			I[i][j] = length(sample); " +
    		"		} \n" +
    		"	} \n" +
    	
    		"	/* calculate the convolution values for all the masks */ \n" +
    		" 	for (int i=0; i<9; i++) { \n" +
    		"		float dp3 = dot(G[i][0], I[0]) + dot(G[i][1], I[1]) + dot(G[i][2], I[2]); \n" +
    		"		cnv[i] = dp3 * dp3;  \n" +
    		"	} \n" +

    		"	float M = (cnv[0] + cnv[1]) + (cnv[2] + cnv[3]); \n" +
    		"	float S = (cnv[4] + cnv[5]) + (cnv[6] + cnv[7]) + (cnv[8] + M);  \n" +
    	
			//" 	vec4 coloroutput = vec4(0,0,0,1);\n" +
			"	vec4 coloroutput = texture2D( image, vTextureCoord);\n" +
			"	vec4 edge = vec4(1.0) - vec4(sqrt(M/S));\n" +
			"   if (all(lessThan(vec3(.90, .90, .90), vec3(edge.x, edge.y, edge.z))))\n" +
//			"    	coloroutput = texture2D( image, vTextureCoord);\n" +
			"    	coloroutput = vec4( (coloroutput.x +((1.0-coloroutput.x)/2.0)), \n" +
			"                           (coloroutput.y +((1.0-coloroutput.y)/2.0)), \n" +
			"                           (coloroutput.z +((1.0-coloroutput.z)/2.0)), \n" +
			"                           1.0 );\n" +
//			"    	coloroutput = edge;\n" +
			"   else\n" +
//			"		coloroutput = vec4(0.0,0.0,0.0,1.0); \n" +
			"		coloroutput = coloroutput * edge; \n" +
			"	gl_FragColor = coloroutput; \n" +
    		"}";
    
    public static final String mVignetteFragShader =
    		  "#extension GL_OES_EGL_image_external : require\n" +
    		     "precision mediump float;\n" +
    		  "varying vec2 vTextureCoord;\n" +
    		      "uniform samplerExternalOES sTexture;\n" +
    		     "void main() {\n" +
    		  "vec4 color = texture2D(sTexture, vTextureCoord.st);\n" +
    		  "vec2 distance = vec2(0.5, 0.5) - vTextureCoord.st;\n" +
    		   "color.rgb = color.rgb * ((0.4 - dot(distance, distance)) * 2.8);\n" +
    		  "gl_FragColor = color;}\n";

    public static final String mSepiaFragShader =
    		  "#extension GL_OES_EGL_image_external : require\n" +
    		      "precision mediump float;\n" +
    		  "varying vec2 vTextureCoord;\n" +
    		     "uniform samplerExternalOES sTexture;\n" +
    		     "void main() {\n" +
    		  "vec4 color = texture2D(sTexture, vTextureCoord.st);\n" +
    		   "// Convert to gray + desaturated Sepia\n" +
    		  "float gray = dot(color, vec4(0.3, 0.59, 0.11, 0.0));\n" + 
    		  "color = vec4(gray * vec3(0.9, 0.8, 0.6), 1.0);\n" +
    		  "gl_FragColor = color;}\n";

    public static String msavetofileFragShader = 
    		"#extension GL_OES_EGL_image_external : require\n" +
    			"precision mediump float;\n" +
    			"varying vec2 vTextureCoord;\n" +
    			"uniform samplerExternalOES sTexture;\n" +
    			"void main() {\n" +
    			"vec4 color = texture2D(sTexture, vTextureCoord.st).rgba;\n" +
    			//"vec3 gammaColor = pow(color.rgb, vec3(0.4545,0.4545,0.4545));\n"+
    			
    			//"float luma = dot(gammaColor.rgb, vec3(0.299, 0.587, 0.114));\n" +
    			//"float luma = dot(gammaColor.rgb, vec3(0.25, 0.5, 0.25));\n" +
    		    //"float cr = dot(color.rgb, vec3(-0.147, -0.289, 0.436));\n" +
    			//"float cb = dot(color.rgb, vec3(0.615, -0.515, -0.1));\n" +
    		    
    		    //"float cb = 128.0 + dot(gammaColor.rgb, vec3(-0.148, -0.291, 0.439));\n" +
    			//"float cr = 128.0 + dot(gammaColor.rgb, vec3(0.439, -0.368, -0.0714));\n" +
    			
   		    	//"float cr = dot(color.rgb, vec3(-0.0999, -0.3361, 0.436));\n" +
    			//"float cb = dot(color.rgb, vec3(0.615, -0.5586, -0.0564));\n" +
    			
    			//"float cb = dot(gammaColor.rgb, vec3(-0.169, -0.331, 0.5));\n" +
    			//"float cr = dot(gammaColor.rgb, vec3(0.5, -0.419, -0.081));\n" +
			
    			//"float cb = 128.0 + dot(gammaColor.rgb, vec3(-37.797, -74.203, 112.0));\n" +
				//"float cr = 128.0 + dot(gammaColor.rgb, vec3(112.0, -93.786, -18.214));\n" +
				//"float cr = 0.7132*(gammaColor.r - luma);\n" +
				//"float cb = 0.5647*(gammaColor.b - luma);\n" +
				//"gl_FragColor = vec4(1.0, luma, cr, cb);}\n";
    			//"gl_FragColor = vec4(cr, cb, luma, 1.0);}\n";
				
    			//"float cr = color.r * 1.0 ;\n" +
    			//"float cb = color.b * 1.0 ;\n" +
    			
   		    	//"float cr = 0.5 + dot(gammaColor.rgb, vec3(-0.25, 0.5, -0.25));\n" +
    			//"float cb = 0.5 + dot(gammaColor.rb, vec2(0.5, -0.5));\n" +
    			
    			
    			//"gl_FragColor = vec4(luma, cr, cb, 1.0);}\n";
    			//"gl_FragColor = vec4(cb, luma, cr, 1.0);}\n";
    			"gl_FragColor = color;}\n";

	public static String [] shaderArray = {mGrayScaleFragShader, mPixellateFragShader, 
											mTileFragShader, mPosterizeFragShader, 
											laplacianFragShaderCode, neonFragShaderCode,
											mKuwaharaFragShader, freichenFragShaderCode,
											cartoonifyFragShaderCode, cartoonifyFragShaderCode,
											mSepiaFragShader};
}

/*
*  This fragment shader samples from a 2D texture and converts
*  the RGBA value into grayscale by NTSC conversion weights.
*/
#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES sTexture; 
varying vec2 vTextureCoord;

void main()
{
	vec4 color = texture2D(sTexture, vTextureCoord.st).rgba;
	
	float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    
    gl_FragColor = vec4(luma, luma, luma, 1.0);
 
}
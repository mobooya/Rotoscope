/*
*  This fragment shader pixellates an image by replicating
*  a pixel's color into a rectangle around it.
*
*  Source code copied from http://coding-experiments.blogspot.com/2010/06/pixelation.html
*/


uniform sampler2D tex;

void main()
{
 float dx = 15.*(1./512.);
 float dy = 10.*(1./512.);
 vec2 coord = vec2(dx*floor(gl_TexCoord[0].x/dx),
                   dy*floor(gl_TexCoord[0].y/dy));
 gl_FragColor = texture2D(tex, coord);
}

attribute vec2 Position;
attribute vec2 TexCoord;
uniform mat4 ProjMatrix;
uniform mat4 MVMatrix;

void main(void)
{
  gl_TexCoord[0].xy = TexCoord.xy;
  gl_Position = ProjMatrix * MVMatrix * vec4(Position.st, 0, 1);
}
// maximum size supported by this shader
const int MaxKernelSize = 25;

// array of offsets for accessing the base image
uniform vec2 Offset[MaxKernelSize];

// size of kernel (width * height) for this execution
int KernelSize = 25;

// value for each location in the convolution kernel
uniform float KernelValue[MaxKernelSize];

// image to be convolved
uniform sampler2D sceneTex;

void main()
{
    int i;
    
    vec4 color = texture2D(sceneTex, gl_TexCoord[0].st).rgba;
    vec4 sum = vec4(0.0);
	vec4 output = vec4(0,0,0,1);
	
    for (i = 0; i < KernelSize; i++)
    {
        vec4 tmp = texture2D(sceneTex, gl_TexCoord[0].st + Offset[i]);
        vec4 v = vec4(KernelValue[i], KernelValue[i], KernelValue[i], KernelValue[i]);
        sum += tmp * v;
    }
    
    if (all(greaterThan(vec3(0.40, 0.40, 0.40), vec3(sum.x, sum.y, sum.z))))
    	output = color;
    else
    	output = vec4(0, 0, 0, 1.0);
    
    gl_FragColor = output;
}


#define MAX_LIGHTS 6

attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
uniform mat4 u_projTrans;
uniform int lightCount;
uniform vec3 lights[MAX_LIGHTS];
uniform vec2 resolution;
varying vec4 v_color;
varying vec2 v_texCoords;
varying vec3 v_lightDirs[MAX_LIGHTS];
varying float v_lightDists[MAX_LIGHTS];

void main()
{
   vec4 vertexScreen = u_projTrans * a_position;
   vec2 vertexWindow = vec2(vertexScreen.xy);
   vertexWindow.x = resolution.x * (vertexWindow.x + 1.0) / 2.0;
   vertexWindow.y = resolution.y * (vertexWindow.y + 1.0) / 2.0;
   
   for(int i = 0; i < lightCount; i++) {
     vec3 lightDir = vec3(lights[i].xy - vertexWindow, lights[i].z);
     v_lightDists[i] = length(lightDir) / (resolution.x);
     lightDir = normalize(lightDir);
     v_lightDirs[i] = normalize(vec3(lightDir.xy, lights[i].z));
   }
   
   v_color = a_color;
   v_texCoords = a_texCoord0;
   gl_Position = vertexScreen;
}
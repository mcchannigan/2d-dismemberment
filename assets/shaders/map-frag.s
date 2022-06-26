#define MAX_LIGHTS 6
#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
varying vec3 v_lightDirs[MAX_LIGHTS];
varying float v_lightDists[MAX_LIGHTS];
uniform sampler2D u_texture;
uniform sampler2D u_normals;
uniform vec3 ambientColor;
uniform float ambientIntensity; 
uniform vec2 resolution;
uniform int lightCount;
uniform vec3 lightColors[MAX_LIGHTS];
uniform vec3 attenuation;
uniform mat3 normalMatrix;

void main() {
  vec4 color = texture2D(u_texture, v_texCoords.st);
  vec3 nColor = texture2D(u_normals, v_texCoords.st).rgb;

  vec3 normal = normalize(nColor * 2.0 - 1.0);

  vec3 lightSum = vec3(0.0);
  /* Manually unrolled for loop because stupid nvidia driver */
  if(lightCount > 0) {
    float d = v_lightDists[0];
    vec3 lightDir = v_lightDirs[0];
    float lambert = clamp(dot(normal, lightDir), 0.0, 1.0);
    float att = 1.0 / ( attenuation.x + (attenuation.y*d) + (attenuation.z*d*d) );
    lightSum += ((lightColors[0].rgb * lambert) * att);
    
    if(lightCount > 1) {
      float d = v_lightDists[1];
      vec3 lightDir = v_lightDirs[1];
      float lambert = clamp(dot(normal, lightDir), 0.0, 1.0);
      float att = 1.0 / ( attenuation.x + (attenuation.y*d) + (attenuation.z*d*d) );
      lightSum += ((lightColors[1].rgb * lambert) * att);
      
      if(lightCount > 2) {
        float d = v_lightDists[2];
	    vec3 lightDir = v_lightDirs[2];
	    float lambert = clamp(dot(normal, lightDir), 0.0, 1.0);
	    float att = 1.0 / ( attenuation.x + (attenuation.y*d) + (attenuation.z*d*d) );
	    lightSum += ((lightColors[2].rgb * lambert) * att);
      }
    }
  }
  
  vec3 result = (ambientColor * ambientIntensity) + lightSum;
  result *= color.rgb;
  
  gl_FragColor = v_color * vec4(result, color.a);
}
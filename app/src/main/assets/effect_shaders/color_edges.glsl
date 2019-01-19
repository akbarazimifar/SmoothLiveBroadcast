
precision mediump float;
varying mediump vec2 vTextureCoord;
//uniform lowp float u_color;

//float d = sin(u_color * 5.0)*0.5 + 1.5; // kernel offset

//vec4 getTexel(vec2 aPosition) {
//	return texture2D(sTexture, aPosition);
//}


void main() {
//    lowp vec4 texel = getTexel(vTextureCoord);
    lowp vec4 texel = texture2D(sTexture, vTextureCoord);
    gl_FragColor = texel;
}

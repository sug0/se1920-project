# Objetivos

No final deste trabalho, espero ter um sistema funcional no qual os
valores (inteiros) da luz provenientes das diversas fontes luminosas do
Minecraft sejam consumíveis por um dispositívo físico que os interprete,
e os use para cotrolar o nível de luz de uma ou várias lâmpadas no
"mundo real".

Além disso, espero que seja possível usar o giroscópio e acelerómetro de
um telemóvel Android para controlar certos aspetos do jogo.

# Requisitos

Serão necessários os seguintes componentes, tanto de *software*, como
*hardware*:

$\CheckedBox$ Telemóvel com Android `\newline`{=tex} $\CheckedBox$
Raspberry Pi ou dispositivo equivalente, que seja capaz de hospedar um
*broker* MQTT `\newline`{=tex} $\CheckedBox$ Jogo Minecraft
`\newline`{=tex}$\CheckedBox$ SDK não oficial para modificar o jogo
Minecraft `\newline`{=tex} $\Box$ Dispositivo Arduino `\newline`{=tex}
$\Box$ Lâmpada LED `\newline`{=tex}$\CheckedBox$ *Breadboard*
`\newline`{=tex}$\Box$ Fios para interligar os componentes digitais

Devido à falta de alguns componentes, nomeadamente relativos ao *output*
da luz no "mundo real", seria necessário simulá-los num ambiente Arduino
virtual @virtual_arduino. Estes requisitos teriam de ser discutidos com
os docentes.

# Arquitetura

```{=tex}
\begin{figure}
    \centering
    \includegraphics[width=0.75\textwidth,page=1]{arch.pdf}
    \caption{A arquitetura do sistema}
\end{figure}
```
A imagem acima ilustra a arquitetura a implementar. Eis uma pequena
legenda:

-   Amarelo -- a lâmpada a controlar
-   Azul -- o dispositivo Arduino
-   Vermelho -- o Raspberry Pi
-   Verde -- o jogo Minecraft

# Referências

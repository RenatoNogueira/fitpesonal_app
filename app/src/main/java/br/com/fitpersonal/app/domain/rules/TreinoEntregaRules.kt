package br.com.fitpersonal.app.domain.rules

import br.com.fitpersonal.app.domain.model.DivisaoTreino
import br.com.fitpersonal.app.domain.model.ExercicioTreino

/** Lógica do navegador para registrar sessões e gerar o código do resumo (sem dependências externas). */
private const val JS_RETORNO = """
var pacote=null;
try{pacote=JSON.parse(document.getElementById('pacote').textContent);}catch(e){}
if(pacote){
document.querySelectorAll('input.c').forEach(function(i){
  try{i.value=localStorage.getItem(P+'c'+i.dataset.ck)||'';}catch(e){}
  i.addEventListener('input',function(){try{localStorage.setItem(P+'c'+i.dataset.ck,i.value);}catch(e){}});
});
function lerExec(){try{return JSON.parse(localStorage.getItem(P+'exec')||'[]');}catch(e){return [];}}
function gravarExec(l){try{localStorage.setItem(P+'exec',JSON.stringify(l));}catch(e){}}
function hoje(){var d=new Date();var m=d.getMonth()+1,dd=d.getDate();return d.getFullYear()+'-'+(m<10?'0':'')+m+'-'+(dd<10?'0':'')+dd;}
function uuid(){if(window.crypto&&crypto.randomUUID){return crypto.randomUUID();}var s='';for(var i=0;i<32;i++){s+=Math.floor(Math.random()*16).toString(16);}return s;}
document.querySelectorAll('button.fim').forEach(function(b){b.addEventListener('click',function(){
  var d=b.dataset.d;var sec=document.getElementById('s'+d);
  var esf=parseInt(document.getElementById('e'+d).value,10);
  var itens=[];
  sec.querySelectorAll('input[data-k]').forEach(function(i){
    var c=sec.querySelector('input.c[data-ck="'+i.dataset.k+'"]');
    var v=c?parseFloat(c.value.replace(',','.')):NaN;
    itens.push({exercicio:i.dataset.n,cargaKg:(isNaN(v)||v<0||v>2000)?null:v,reps:null,feito:i.checked});
  });
  var l=lerExec();
  l.push({execucaoId:uuid(),data:hoje(),treinoId:pacote.treinoId,divisao:d,esforco:isNaN(esf)?null:esf,itens:itens});
  gravarExec(l);
  sec.querySelectorAll('input[data-k]').forEach(function(i){i.checked=false;try{localStorage.setItem(P+i.dataset.k,'0');}catch(e){}});
  alert('Sessão registrada! Total de sessões: '+l.length);
});});
function b64url(bytes){var s='';for(var i=0;i<bytes.length;i++){s+=String.fromCharCode(bytes[i]);}return btoa(s).split('+').join('-').split('/').join('_').split('=').join('');}
function codificar(json){
  var bytes=new TextEncoder().encode(json);
  if(window.CompressionStream){
    try{
      var cs=new CompressionStream('deflate-raw');
      var w=cs.writable.getWriter();w.write(bytes);w.close();
      return new Response(cs.readable).arrayBuffer().then(function(buf){return 'RJE1:'+b64url(new Uint8Array(buf));});
    }catch(e){}
  }
  return Promise.resolve('RJE0:'+b64url(bytes));
}
document.getElementById('enviar').addEventListener('click',function(){
  var l=lerExec();
  if(!l.length){alert('Registre ao menos uma sessão antes de enviar.');return;}
  var datas=l.map(function(x){return x.data;}).sort();
  var msg=document.getElementById('msg').value.trim();
  var r={formato:1,tipo:'resumo',resumoId:uuid(),alunoRef:pacote.aluno.ref,periodo:{de:datas[0],ate:datas[datas.length-1]},sessoes:l,mensagem:msg?msg.slice(0,1900):null};
  codificar(JSON.stringify(r)).then(function(cod){
    var texto='Meu resumo de treino:\n'+cod;
    if(navigator.share){navigator.share({text:texto}).catch(function(){});}
    else if(navigator.clipboard){navigator.clipboard.writeText(texto).then(function(){alert('Código copiado. Cole na conversa com seu professor.');});}
    else{prompt('Copie o código e envie ao professor:',texto);}
  });
});
}
"""

/**
 * Formas de entregar o treino ao aluno sem o aluno precisar instalar nada:
 * texto formatado (WhatsApp) e uma página HTML única que abre no navegador do celular e funciona offline.
 */
object TreinoEntregaRules {

    /** Escapa texto digitado pelo profissional antes de entrar no HTML. */
    fun escapar(s: String): String = buildString(s.length + 8) {
        for (c in s) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(c)
        }
    }

    private fun detalhes(e: ExercicioTreino): List<String> = listOfNotNull(
        MetodoTreino.de(e.metodo)?.rotulo,
        e.series?.takeIf { it.isNotBlank() }?.let { "$it séries" },
        e.repeticoes?.takeIf { it.isNotBlank() }?.let { "$it reps" },
        e.carga?.takeIf { it.isNotBlank() }?.let { "carga $it" },
        e.descanso?.takeIf { it.isNotBlank() }?.let { "descanso $it" },
    )

    /** Treino em texto simples com *negrito* do WhatsApp. Divisões vazias não aparecem. */
    fun texto(
        nomeAluno: String,
        nomeTreino: String,
        objetivo: String?,
        grupos: Map<DivisaoTreino, List<ExercicioTreino>>,
        profissional: String,
    ): String = buildString {
        append("*").append(nomeTreino).append("*\n")
        append("Aluno(a): ").append(nomeAluno).append('\n')
        objetivo?.takeIf { it.isNotBlank() }?.let { append("Objetivo: ").append(it).append('\n') }
        DivisaoTreino.entries.forEach { d ->
            val lista = grupos[d].orEmpty()
            if (lista.isEmpty()) return@forEach
            append("\n*Treino ").append(d.name).append("*\n")
            lista.forEachIndexed { i, e ->
                append(i + 1).append(". ").append(e.nome)
                val det = detalhes(e)
                if (det.isNotEmpty()) append(" — ").append(det.joinToString(", "))
                append('\n')
                e.observacoes?.takeIf { it.isNotBlank() }?.let { append("   _").append(it).append("_\n") }
                e.video?.takeIf { it.isNotBlank() }?.let { append("   ▶ ").append(it).append('\n') }
            }
        }
        if (profissional.isNotBlank()) append("\n").append(profissional)
    }.trimEnd()

    /** Página HTML autossuficiente (sem internet, sem scripts externos). Marcações de "feito" ficam no navegador. */
    fun html(
        nomeAluno: String,
        nomeTreino: String,
        objetivo: String?,
        dataTexto: String,
        profissional: String,
        marca: String,
        grupos: Map<DivisaoTreino, List<ExercicioTreino>>,
        /** `.fittreino` embutido: habilita registrar sessões e devolver o resumo por código. */
        pacoteJson: String? = null,
    ): String {
        val divisoes = DivisaoTreino.entries.filter { grupos[it].orEmpty().isNotEmpty() }
        val prefixo = "rje-" + (nomeAluno + nomeTreino + dataTexto).hashCode().toUInt().toString(16)
        val sb = StringBuilder(4096)

        sb.append("<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\">")
        sb.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
        sb.append("<title>").append(escapar(nomeTreino)).append("</title><style>")
        sb.append(
            "body{margin:0;font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;background:#edf6f5;color:#0f1b1f}" +
                "header{background:linear-gradient(135deg,#0a8579,#0891b2);color:#fff;padding:20px 16px}" +
                "h1{margin:0 0 4px;font-size:22px}header p{margin:2px 0;opacity:.92;font-size:14px}" +
                "nav{display:flex;gap:8px;padding:12px 16px;position:sticky;top:0;background:#edf6f5}" +
                "nav button{flex:1;border:0;border-radius:14px;padding:12px 0;font-size:16px;font-weight:700;" +
                "background:#fff;color:#0a8579;box-shadow:0 1px 2px rgba(0,0,0,.12)}" +
                "nav button.on{background:#0a8579;color:#fff}" +
                "main{padding:0 16px 32px}section{display:none}section.on{display:block}" +
                "label{display:flex;gap:12px;align-items:flex-start;background:#fff;border-radius:16px;" +
                "padding:14px;margin:10px 0;box-shadow:0 1px 2px rgba(0,0,0,.08)}" +
                "input{width:24px;height:24px;margin-top:2px;accent-color:#0a8579;flex:none}" +
                "label b{display:block;font-size:16px}label span{display:block;font-size:14px;color:#44545a;margin-top:2px}" +
                "label em{display:block;font-size:13px;color:#5f6368;margin-top:4px}" +
                "input:checked+div{opacity:.55;text-decoration:line-through}" +
                "footer{padding:0 16px 24px;font-size:12px;color:#5f6368}" +
                "input.c{width:100%;box-sizing:border-box;margin:-4px 0 8px;padding:10px;border:1px solid #cad6da;" +
                "border-radius:12px;font-size:15px}" +
                ".fim{display:flex;gap:8px;margin:12px 0}.fim select,.fim button,.envio button{padding:12px;" +
                "border-radius:14px;font-size:15px;border:1px solid #cad6da}" +
                ".fim button,.envio button{flex:1;border:0;background:#0a8579;color:#fff;font-weight:700}" +
                ".envio{padding:0 16px 16px}.envio textarea{width:100%;box-sizing:border-box;min-height:70px;" +
                "border:1px solid #cad6da;border-radius:12px;padding:10px;font-size:15px;margin-bottom:8px}",
        )
        sb.append("</style></head><body><header><h1>").append(escapar(nomeTreino)).append("</h1>")
        sb.append("<p>").append(escapar(nomeAluno)).append(" • ").append(escapar(dataTexto)).append("</p>")
        objetivo?.takeIf { it.isNotBlank() }?.let { sb.append("<p>Objetivo: ").append(escapar(it)).append("</p>") }
        val assinatura = listOf(profissional, marca).filter { it.isNotBlank() }.joinToString(" • ")
        if (assinatura.isNotEmpty()) sb.append("<p>").append(escapar(assinatura)).append("</p>")
        sb.append("</header>")

        if (divisoes.isEmpty()) {
            sb.append("<main><p>Este treino ainda não tem exercícios.</p></main>")
        } else {
            sb.append("<nav>")
            divisoes.forEachIndexed { i, d ->
                sb.append("<button data-d=\"").append(d.name).append("\"")
                if (i == 0) sb.append(" class=\"on\"")
                sb.append(">").append(d.name).append("</button>")
            }
            sb.append("</nav><main>")
            divisoes.forEachIndexed { i, d ->
                sb.append("<section id=\"s").append(d.name).append("\"")
                if (i == 0) sb.append(" class=\"on\"")
                sb.append(">")
                grupos[d].orEmpty().forEachIndexed { j, e ->
                    sb.append("<label><input type=\"checkbox\" data-k=\"").append(d.name).append(j)
                        .append("\" data-n=\"").append(escapar(e.nome)).append("\">")
                    sb.append("<div><b>").append(escapar(e.nome)).append("</b>")
                    val det = detalhes(e)
                    if (det.isNotEmpty()) sb.append("<span>").append(escapar(det.joinToString(" • "))).append("</span>")
                    e.observacoes?.takeIf { it.isNotBlank() }?.let {
                        sb.append("<em>").append(escapar(it)).append("</em>")
                    }
                    sb.append("</div></label>")
                    e.video?.takeIf { it.isNotBlank() }?.let {
                        sb.append("<a class='v' href='").append(escapar(it)).append("' target='_blank' rel='noopener'>▶ Ver vídeo</a>")
                    }
                    if (pacoteJson != null) {
                        sb.append("<input class=\"c\" data-ck=\"").append(d.name).append(j)
                            .append("\" inputmode=\"decimal\" placeholder=\"Carga usada (kg)\">")
                    }
                }
                if (pacoteJson != null) {
                    sb.append("<div class=\"fim\"><select id=\"e").append(d.name).append("\">")
                    sb.append("<option value=\"\">Esforço</option>")
                    for (n in 1..10) sb.append("<option>").append(n).append("</option>")
                    sb.append("</select><button class=\"fim\" data-d=\"").append(d.name)
                        .append("\">Registrar sessão ").append(d.name).append("</button></div>")
                }
                sb.append("</section>")
            }
            sb.append("</main>")
        }
        if (pacoteJson != null && divisoes.isNotEmpty()) {
            sb.append("<div class=\"envio\"><textarea id=\"msg\" placeholder=\"Quer contar algo ao professor? (opcional)\">")
            sb.append("</textarea><button id=\"enviar\">Enviar resumo ao professor</button></div>")
            // </ dentro de <script> encerraria o bloco: escapa a barra (JSON.parse aceita \/).
            sb.append("<script type=\"application/json\" id=\"pacote\">")
                .append(pacoteJson.replace("</", "<\\/")).append("</script>")
        }
        sb.append("<footer>Toque nos exercícios para marcar como feitos. As marcações ficam salvas só neste aparelho.</footer>")
        sb.append("<script>(function(){var P='").append(prefixo).append("-';")
        sb.append(
            "var bs=document.querySelectorAll('nav button');" +
                "bs.forEach(function(b){b.addEventListener('click',function(){" +
                "bs.forEach(function(x){x.classList.remove('on')});b.classList.add('on');" +
                "document.querySelectorAll('section').forEach(function(s){s.classList.remove('on')});" +
                "document.getElementById('s'+b.dataset.d).classList.add('on');})});" +
                "document.querySelectorAll('input[data-k]').forEach(function(i){" +
                "try{i.checked=localStorage.getItem(P+i.dataset.k)==='1';}catch(e){}" +
                "i.addEventListener('change',function(){try{localStorage.setItem(P+i.dataset.k,i.checked?'1':'0');}catch(e){}});});",
        )
        if (pacoteJson != null && divisoes.isNotEmpty()) sb.append(JS_RETORNO)
        sb.append("})();</script></body></html>")
        return sb.toString()
    }
}

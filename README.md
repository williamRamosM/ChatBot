# ChatBot

Chat de desktop em Java Swing com personagens e respostas faladas. O backend em Python usa Gemini para gerar o texto e `edge-tts` para sintetizar a voz.

## Como rodar no Windows

Você precisa de **Python 3.13**, **JDK 25**, acesso à internet e uma chave da API do Gemini.

1. Na raiz do projeto, rode no PowerShell:

   ```powershell
   python -m venv .venv
   .\.venv\Scripts\python.exe -m pip install -r requirements.txt
   ```

2. Crie `src/backend/python/.env` com a chave do Gemini. As duas variáveis da Cloudflare são opcionais e servem para o botão **Gerar imagem falando**.

   ```dotenv
   GEMINI_API_KEY=sua_chave_do_gemini
   # CLOUDFLARE_ACCOUNT_ID=seu_account_id
   # CLOUDFLARE_API_TOKEN=seu_token
   ```

   Para usar a geração de imagens, tire o `#` das duas linhas da Cloudflare e preencha os valores. Guarde as chaves no `.env` local.

3. Inicie o backend na raiz do projeto e deixe o terminal aberto:

   ```powershell
   .\.venv\Scripts\python.exe -m uvicorn main:app --app-dir src/backend/python --env-file src/backend/python/.env --reload
   ```

4. No IntelliJ, abra o `pom.xml` como projeto Maven, selecione o **JDK 25** e execute `src/frontend/java/Main.java` com a raiz do projeto como diretório de trabalho.

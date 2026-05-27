# Docker e permissões no Linux / WSL2

Guia enxuto para evitar `permission denied while trying to connect to the Docker daemon socket` e problemas de path no WSL2. Cobre a maior parte dos casos de desenvolvimento local (incluindo este projeto).

---

## Porque o erro acontece

O socket do daemon costuma ser `/var/run/docker.sock`, com permissões típicas `root:docker` e modo `660`. Só o root e utilizadores no grupo `docker` falam com o daemon sem `sudo`.

---

## Solução definitiva (utilizador no grupo `docker`)

```bash
sudo usermod -aG docker "$USER"
```

Depois é **obrigatório** recarregar os grupos da sessão:

- **Encerrar sessão e voltar a entrar**, ou **reiniciar** (inclui “reiniciar WSL”: `wsl --shutdown` no Windows e abrir de novo).
- **`newgrp docker`** só activa o grupo **naquele terminal** (e processos filhos). Outros terminais já abertos continuam sem `docker` até novo login.

**Por que logout?** O Linux aplica os grupos do utilizador no **login**. Alterar grupos com `usermod` não actualiza sessões já abertas.

**Verificação:** se após login `groups` **não** listar `docker`, a sessão ainda não recarregou os grupos.

---

## Solução temporária (não ideal)

```bash
sudo docker compose up -d
```

Funciona, mas corre o daemon como root do lado dos efeitos em ficheiros: em bind mounts, ficheiros criados pelo container podem ficar `root:root` e atrapalhar `git` e edição. Use só enquanto não aplicar a solução definitiva.

---

## Checklist: permissão e socket

1. **Grupo**

   ```bash
   groups
   ```

   Deve aparecer `docker` na lista.

2. **Socket**

   ```bash
   ls -la /var/run/docker.sock
   ```

   Esperado: algo como `srw-rw---- 1 root docker …`

3. **Teste sem sudo**

   ```bash
   docker ps
   ```

   Se listar (mesmo vazio) sem erro, está correcto.

4. **Ainda `denied` após novo login**

   - Tentar **reiniciar o serviço Docker** (depende da instalação), por exemplo:

     ```bash
     sudo systemctl restart docker
     ```

     No WSL2 sem systemd, o método varia; reiniciar WSL (`wsl --shutdown`) costuma repor estado.

   - **Último recurso** (estado estranho do socket, por exemplo após updates):

     ```bash
     sudo chown root:docker /var/run/docker.sock
     sudo chmod 660 /var/run/docker.sock
     ```

---

## Paths no WSL2: `/mnt/c` vs home Linux

| Path | Performance | Permissões POSIX | Uso recomendado |
|------|-------------|------------------|-----------------|
| `/mnt/c/Users/...` | Lenta (9p/NTFS) | Problemática com bind mount | Evitar para repo + volumes |
| `~/projetos/...` ou `/home/...` | ext4 nativo | Comportamento esperado | Preferir |

**Porquê:** `/mnt/c` não é um filesystem Linux “de verdade” para o Docker; builds e volumes ficam lentos e surgem `permission denied` em cenários comuns.

**Se o projecto só existir em Windows:**

```bash
mkdir -p ~/projetos
cp -a "/mnt/c/Users/SEU_USER/Desktop/Loja Sistema" ~/projetos/loja
cd ~/projetos/loja
docker compose up -d db redis
```

(Ajusta o path de origem. Para trabalhar sempre no mesmo sítio, preferir `git clone` directamente para `~/projetos`.)

---

## Setup rápido: Docker Engine em Ubuntu / WSL (distro Linux)

Opção comum **só na distro** (sem depender do Docker Desktop no Windows):

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER"
```

Sair da sessão WSL/Ubuntu e voltar (ou `wsl --shutdown` no Windows). Depois:

```bash
docker run --rm hello-world
```

**Nota:** No WSL2 também é frequente usar **Docker Desktop** com integração na distro; é outra combinação válida. Engine instalado **dentro** da distro costuma ser mais previsível para paths e permissões 100% Linux.

**Regra prática:** depois do grupo `docker` estar activo, **não** é necessário `sudo docker` no dia-a-dia. Se precisares sempre de `sudo`, o setup ou a sessão ainda não está alinhado com este guia.

---

## Ligação com este repositório

Na raiz do projecto (já no path correcto em Linux/WSL):

```bash
bash scripts/docker-wsl-check.sh   # opcional: confirma Docker sem sudo
docker compose up -d db redis      # só Postgres + Redis; ver secção abaixo
./mvnw test
./mvnw spring-boot:run
```

Detalhes de portas, contentores e conflito API Docker vs Maven: secção **Rodando o Loja Sistema** abaixo; também `AGENTS.md` na raiz.

---

## Resumo rápido (referência)

Útil para colar em notas ou relembrar sem reler o documento inteiro.

| Tema | Acção |
|------|--------|
| Erro no socket | Por defeito `/var/run/docker.sock` é `root:docker` `660` — entra no grupo `docker` ou usas `sudo` |
| Definitivo | `sudo usermod -aG docker "$USER"` → **logout/login** ou **reboot** / `wsl --shutdown` |
| `newgrp docker` | Só afecta **aquele** terminal |
| Temporário | `sudo docker compose up -d` — risco de ficheiros `root:root` em volumes |
| Checklist | `groups` → `ls -la /var/run/docker.sock` → `docker ps` sem sudo → se falhar, `systemctl restart docker` ou último recurso `chown`/`chmod` no socket |
| WSL path | Evitar projecto + volumes em `/mnt/c/...`; preferir `~/projetos/...` (ext4) |
| Instalação limpa (distro) | `get.docker.com` (`curl -fsSL … \| sh`) + `usermod` + novo login + `docker run --rm hello-world` |
| Regra | Com grupo `docker` activo, **não** precisas de `sudo docker` no quotidiano; se precisares sempre, o setup ou a sessão está incorrecto |

Guia completo acima; **Docker Desktop** no Windows com integração WSL também é opção válida (ver secção “Setup rápido”).

---

## Rodando o Loja Sistema — fluxo recomendado

### 1. Subir apenas dependências

Para desenvolvimento local com Maven, suba só Postgres e Redis:

```bash
docker compose up -d db redis
```

**Motivo:** O `docker-compose.yml` define três serviços: `db`, `redis` e `api`. Com `docker compose up -d` (sem filtro), o contentor `loja-api` expõe a API na porta **8000** do host (alinhada a `application.yml` e ao proxy Vite).

Se correr `./mvnw spring-boot:run` em paralelo com o serviço `api`, **ambos disputam a porta 8000** e usam o mesmo Postgres/Redis — evite (dados inconsistentes, cache Redis desalinhado, logs duplicados).

**Regra:** ou sobe a API no Docker (`docker compose up -d`), ou corre a API só com `./mvnw`. Para o fluxo habitual de desenvolvimento neste repo, preferir **`db` + `redis` no Compose** e **API com Maven**.

### 2. Validar contentores

```bash
docker ps
```

Esperado (apenas dependências):

| Contentor       | IMAGE              | STATUS        |
|-----------------|--------------------|---------------|
| `loja-postgres` | `postgres:16`      | Up (healthy)  |
| `loja-redis`    | `redis:7-alpine`   | Up            |

**Nota:** Redis não tem `healthcheck` no compose actual. **Up** (sem `(healthy)`) é o normal.

### 3. Troubleshooting

```bash
# Logs do Postgres (nome do *serviço* no compose é `db`)
docker compose logs db

# Logs do Redis
docker compose logs redis

# Se a API Docker tiver subido sem querer
docker compose stop api
```

### 4. Portas do projecto

| Ambiente                 | Porta |
|--------------------------|-------|
| Maven local (`./mvnw`)   | 8000  |
| Contentor `loja-api`     | 8000  |
| Postgres                 | 5432  |
| Redis                    | 6379  |

Health check local (API em Maven): `curl -i http://localhost:8000/actuator/health`. Swagger: `http://localhost:8000/swagger-ui.html`.

---

*Última revisão: alinhamento com permissões POSIX, WSL2, fluxo `db redis` + Maven e boas práticas de desenvolvimento local.*

FROM mcr.microsoft.com/dotnet/aspnet:9.0-preview AS base
WORKDIR /app
EXPOSE 8080

FROM mcr.microsoft.com/dotnet/sdk:9.0-preview AS build
WORKDIR /src
COPY ["ComicApiDod/ComicApiDod.csproj", "ComicApiDod/"]
RUN dotnet restore "ComicApiDod/ComicApiDod.csproj"
COPY . .
WORKDIR "/src/ComicApiDod"
RUN dotnet build "ComicApiDod.csproj" -c Release -o /app/build

FROM build AS publish
RUN dotnet publish "ComicApiDod.csproj" -c Release -o /app/publish /p:UseAppHost=false

FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .

# Install EF Core tools
RUN dotnet tool install --global dotnet-ef
ENV PATH="${PATH}:/root/.dotnet/tools"

# Create entrypoint script to run migrations and start the app
COPY docker-entrypoint.sh /app/
RUN chmod +x /app/docker-entrypoint.sh

ENTRYPOINT ["/app/docker-entrypoint.sh"]


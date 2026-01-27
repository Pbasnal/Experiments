FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

COPY ["ComicApiDod/ComicApiDod.csproj", "ComicApiDod/"]
RUN dotnet restore "ComicApiDod/ComicApiDod.csproj"

COPY ["ComicApiDod/", "ComicApiDod/"]
RUN dotnet build "ComicApiDod/ComicApiDod.csproj" -c Release -o /app/build

RUN dotnet tool install --global dotnet-ef --version 8.0.0
ENV PATH="${PATH}:/root/.dotnet/tools"

# Install EF Core tools
RUN #dotnet ef migrations add InitialCreate --project  "/src/ComicApiDod/ComicApiDod.csproj"
RUN dotnet publish "ComicApiDod/ComicApiDod.csproj" -c Release -o /app/publish
RUN mv /app/publish/ComicApiDod /app/publish/app
RUN ls -la /app/publish


# Build runtime image
FROM mcr.microsoft.com/dotnet/aspnet:8.0
EXPOSE 8080
WORKDIR /app
COPY --from=build /app/publish .
ENTRYPOINT ["dotnet", "ComicApiDod.dll"]
